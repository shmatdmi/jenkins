pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timestamps()
        timeout(time: 10, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30'))
    }

    parameters {
        string(name: 'PROJECT_NAME', defaultValue: 'jenkins', trim: true,
               description: 'Название проекта в SAST')
        string(name: 'RELEASE', defaultValue: '', trim: true,
               description: 'Релиз, например SAST-123. По умолчанию SAST-<номер сборки>')
    }

    environment {
        SAST_URL = 'https://84.54.57.64'
    }

    stages {
        stage('Получение токена') {
            steps {
                script {
                    env.SAST_RELEASE = params.RELEASE ?: "SAST-${env.BUILD_NUMBER}"
                    if (!(env.SAST_RELEASE ==~ /^[A-Za-z]{2,4}-[1-9][0-9]{0,3}$/)) {
                        error('Укажите RELEASE в формате SAST-123: от 2 до 4 латинских букв и число от 1 до 9999.')
                    }
                    if (!params.PROJECT_NAME || params.PROJECT_NAME.length() > 512) {
                        error('PROJECT_NAME должен содержать от 1 до 512 символов.')
                    }
                    env.SAST_TEMP_DIR = sh(returnStdout: true, script: '''
                        set +x
                        umask 077
                        mkdir -p "$WORKSPACE_TMP"
                        mktemp -d "$WORKSPACE_TMP/sast.XXXXXX"
                    ''').trim()
                }

                withCredentials([usernamePassword(
                    credentialsId: 'sast-login',
                    usernameVariable: 'SAST_USERNAME',
                    passwordVariable: 'SAST_PASSWORD'
                )]) {
                    script {
                        if (!env.SAST_USERNAME || !env.SAST_PASSWORD) {
                            error('Заполните логин и пароль в Jenkins credential sast-login.')
                        }
                        writeJSON file: "${env.SAST_TEMP_DIR}/login.json",
                                  json: [username: env.SAST_USERNAME, password: env.SAST_PASSWORD]
                    }
                    sh(label: 'POST /api/auth/login', script: '''
                        set +x
                        umask 077
                        trap 'rm -f "$SAST_TEMP_DIR/login.json" "$SAST_TEMP_DIR/auth.json"' EXIT
                        status=$(curl --silent --show-error \
                            --connect-timeout 15 --max-time 60 \
                            --header 'Content-Type: application/json' \
                            --data-binary "@$SAST_TEMP_DIR/login.json" \
                            --output "$SAST_TEMP_DIR/auth.json" \
                            --write-out '%{http_code}' \
                            "$SAST_URL/api/auth/login")
                        if [ "$status" != 200 ]; then
                            echo "Получение токена: HTTP $status" >&2
                            exit 1
                        fi
                        # CodeSentry issues a base64url session token.
                        token=$(sed -nE 's/.*"access_token"[[:space:]]*:[[:space:]]*"([A-Za-z0-9_-]+)".*/\\1/p' "$SAST_TEMP_DIR/auth.json")
                        if [ -z "$token" ]; then
                            echo 'Ответ авторизации не содержит access_token.' >&2
                            exit 1
                        fi
                        printf 'Authorization: Bearer %s\\n' "$token" > "$SAST_TEMP_DIR/authorization.header"
                        unset token
                    ''')
                }
                echo 'Токен SAST получен.'
            }
        }

        stage('Сканирование SAST') {
            steps {
                checkout scm
                withEnv(["SAST_PROJECT_NAME=${params.PROJECT_NAME}"]) {
                    sh(label: 'Упаковка Git-коммита и POST /api/scans/archive', script: '''
                        set +x
                        umask 077
                        git archive --format=zip --output="$SAST_TEMP_DIR/code.zip" HEAD
                        status=$(curl --silent --show-error \
                            --connect-timeout 15 --max-time 300 \
                            --header "@$SAST_TEMP_DIR/authorization.header" \
                            --form "archive=@$SAST_TEMP_DIR/code.zip;type=application/zip" \
                            --form-string "release=$SAST_RELEASE" \
                            --form-string "projectName=$SAST_PROJECT_NAME" \
                            --output sast-result.json \
                            --write-out '%{http_code}' \
                            "$SAST_URL/api/scans/archive")
                        if [ "$status" != 201 ]; then
                            echo "Сканирование SAST: HTTP $status" >&2
                            exit 1
                        fi
                    ''')
                }
                script {
                    def result = readJSON(file: 'sast-result.json', returnPojo: true)
                    if (!result.scanId || result.summary == null) {
                        error('Ответ SAST не содержит scanId или summary.')
                    }
                    echo "Сканирование ${result.scanId}: файлов ${result.filesScanned}, найдено проблем ${result.summary.total}."
                    currentBuild.description = "${env.SAST_RELEASE}: ${result.summary.total} findings"
                }
                archiveArtifacts artifacts: 'sast-result.json', fingerprint: true
            }
        }
    }

    post {
        always {
            script {
                if (env.SAST_TEMP_DIR) {
                    dir(env.SAST_TEMP_DIR) {
                        deleteDir()
                    }
                }
            }
        }
    }
}
