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
        string(name: 'REPOSITORY_URL', defaultValue: 'https://github.com/shmatdmi/jenkins.git', trim: true,
               description: 'URL Git-репозитория для сканирования: HTTPS или SSH')
        string(name: 'BRANCH', defaultValue: 'educations', trim: true,
               description: 'Название ветки для сканирования, например main или feature/my-branch')
        string(name: 'GIT_CREDENTIALS_ID', defaultValue: '', trim: true,
               description: 'ID Jenkins credentials для Git. Для публичного репозитория оставьте пустым')
        string(name: 'PROJECT_NAME', defaultValue: 'test new image', trim: true,
               description: 'Название проекта в SAST')
        string(name: 'RELEASE', defaultValue: '', trim: true,
               description: 'Обязательный параметр. Укажите релиз перед запуском, например sast-104')
    }

    environment {
        SAST_URL = 'https://84.54.57.64'
    }

    stages {
        stage('Получение токена') {
            steps {
                script {
                    env.SAST_REPOSITORY_URL = params.REPOSITORY_URL?.trim() ?: ''
                    env.SAST_BRANCH = params.BRANCH?.trim() ?: ''
                    def httpUrl = env.SAST_REPOSITORY_URL ==~ /^https?:\/\/[^\/@\s]+\/[^\s]+$/
                    def sshUrl = env.SAST_REPOSITORY_URL ==~ /^ssh:\/\/(?:[A-Za-z0-9._-]+@)?[^\/@\s]+\/[^\s]+$/
                    def scpUrl = env.SAST_REPOSITORY_URL ==~ /^[A-Za-z0-9._-]+@[^:\s\/]+:[^\s]+$/
                    if (!(httpUrl || sshUrl || scpUrl)) {
                        error('Укажите REPOSITORY_URL в формате HTTPS или SSH. Данные доступа задаются через GIT_CREDENTIALS_ID.')
                    }
                    if (!env.SAST_BRANCH) {
                        error('Заполните обязательный параметр BRANCH, например main.')
                    }
                    env.SAST_RELEASE = params.RELEASE?.trim() ?: ''
                    if (!(env.SAST_RELEASE ==~ /^[A-Za-z]{2,4}-[1-9][0-9]{0,3}$/)) {
                        error('Перед запуском заполните обязательный параметр RELEASE, например sast-104: от 2 до 4 латинских букв и число от 1 до 9999.')
                    }
                    if (!params.PROJECT_NAME || params.PROJECT_NAME.length() > 512) {
                        error('PROJECT_NAME должен содержать от 1 до 512 символов.')
                    }
                    sh(label: 'Проверка имени ветки', script: '''
                        set +x
                        if ! git check-ref-format --branch "$SAST_BRANCH" >/dev/null; then
                            echo 'Укажите корректное название ветки в BRANCH.' >&2
                            exit 1
                        fi
                    ''')
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
                dir('sast-source') {
                    deleteDir()
                    script {
                        def remote = [
                            url: env.SAST_REPOSITORY_URL,
                            refspec: "+refs/heads/${env.SAST_BRANCH}:refs/remotes/origin/${env.SAST_BRANCH}"
                        ]
                        if (params.GIT_CREDENTIALS_ID?.trim()) {
                            remote.credentialsId = params.GIT_CREDENTIALS_ID.trim()
                        }
                        def source = checkout(
                            changelog: false,
                            poll: false,
                            scm: [$class: 'GitSCM',
                                  branches: [[name: "refs/remotes/origin/${env.SAST_BRANCH}"]],
                                  userRemoteConfigs: [remote],
                                  extensions: [[$class: 'CloneOption', shallow: true, depth: 1,
                                                noTags: true, honorRefspec: true]]]
                        )
                        env.SAST_COMMIT = source.GIT_COMMIT
                    }
                    sh(label: 'Упаковка выбранного Git-коммита', script: '''
                        set +x
                        umask 077
                        git archive --format=zip --output="$SAST_TEMP_DIR/code.zip" HEAD
                    ''')
                }
                echo "Источник SAST: ${env.SAST_REPOSITORY_URL}, ветка ${env.SAST_BRANCH}, коммит ${env.SAST_COMMIT}."
                withEnv(["SAST_PROJECT_NAME=${params.PROJECT_NAME}"]) {
                    sh(label: 'POST /api/scans/archive', script: '''
                        set +x
                        umask 077
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
                    currentBuild.description = "${env.SAST_RELEASE}: ${env.SAST_BRANCH}, ${result.summary.total} findings"
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
