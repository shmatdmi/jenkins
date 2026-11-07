def githubMetadata(String path, String filename) {
    withEnv(["SAST_GITHUB_API_PATH=${path}", "SAST_GITHUB_API_FILE=${env.SAST_TEMP_DIR}/${filename}"]) {
        def status = sh(label: 'Проверка метаданных GitHub без загрузки кода', returnStdout: true, script: '''
            set +x
            umask 077
            set --
            if [ -s "$SAST_TEMP_DIR/github-authorization.header" ]; then
                set -- --header "@$SAST_TEMP_DIR/github-authorization.header"
            fi
            curl --silent --show-error --connect-timeout 10 --max-time 30 \
                --max-filesize 8388608 \
                --header 'Accept: application/vnd.github+json' \
                --header 'X-GitHub-Api-Version: 2022-11-28' \
                "$@" --output "$SAST_GITHUB_API_FILE" --write-out '%{http_code}' \
                "https://api.github.com$SAST_GITHUB_API_PATH"
        ''').trim()
        if (status != '200') {
            error("Не удалось получить ветку через GitHub API: HTTP ${status}. Проверьте URL, BRANCH и credentials для GitHub API. ZIP не скачивался.")
        }
        return readJSON(file: "${env.SAST_TEMP_DIR}/${filename}", returnPojo: true)
    }
}

def prepareGithubArchive() {
    def repository = env.SAST_REPOSITORY_URL
        .replaceFirst(/(?i)^https?:\/\/github\.com(?::(?:443|80))?\//, '')
        .replaceFirst(/(?i)^ssh:\/\/(?:[A-Za-z0-9._-]+@)?github\.com(?::22)?\//, '')
        .replaceFirst(/(?i)^[A-Za-z0-9._-]+@github\.com:/, '')
        .replaceFirst(/\/$/, '').replaceFirst(/\.git$/, '')
    if (!(repository ==~ /^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/)) {
        error('Загрузка ZIP поддерживается для github.com. Укажите URL репозитория GitHub.')
    }

    def branch = java.net.URLEncoder.encode(env.SAST_BRANCH, 'UTF-8').replace('+', '%20')
    def commit = githubMetadata("/repos/${repository}/branches/${branch}", 'branch-metadata.json')
    env.SAST_COMMIT = commit.commit?.sha ?: ''
    commit = null
    if (!(env.SAST_COMMIT ==~ /^[0-9a-f]{40}$/)) {
        error('GitHub API не вернул коммит выбранной ветки. ZIP не скачивался.')
    }
    env.SAST_ARCHIVE_URL = "https://api.github.com/repos/${repository}/zipball/${env.SAST_COMMIT}"
    echo "Будет загружен только ZIP коммита ${env.SAST_COMMIT}. Git clone, checkout и загрузка объектов Git LFS не используются."
}

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
        string(name: 'GITHUB_API_CREDENTIALS_ID', defaultValue: '', trim: true,
               description: 'Для закрытого GitHub: ID Username with password, пароль — PAT. Для публичного репозитория оставьте пустым')
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
                script {
                    def apiCredentials = params.GITHUB_API_CREDENTIALS_ID?.trim() ?: ''
                    if (!apiCredentials && env.SAST_REPOSITORY_URL.startsWith('http')) {
                        apiCredentials = params.GIT_CREDENTIALS_ID?.trim() ?: ''
                    }
                    if (apiCredentials) {
                        withCredentials([usernamePassword(credentialsId: apiCredentials,
                            usernameVariable: 'GITHUB_API_USERNAME', passwordVariable: 'GITHUB_API_TOKEN')]) {
                            sh(label: 'Настройка доступа к метаданным GitHub', script: '''
                                set +x
                                umask 077
                                printf 'Authorization: Bearer %s\\n' "$GITHUB_API_TOKEN" > "$SAST_TEMP_DIR/github-authorization.header"
                            ''')
                        }
                    }
                    prepareGithubArchive()
                }
                timeout(time: 4, unit: 'MINUTES') {
                    sh(label: 'Скачивание только ZIP, без Git и Git LFS', script: '''
                        set +x
                        umask 077
                        version=$(curl --version | awk 'NR == 1 {print $2}')
                        major=${version%%.*}
                        minor=${version#*.}
                        minor=${minor%%.*}
                        if ! { [ "$major" -gt 8 ] || { [ "$major" -eq 8 ] && [ "$minor" -ge 4 ]; }; }; then
                            echo 'Для ограничения потоковой загрузки нужен curl версии 8.4 или новее.' >&2
                            exit 1
                        fi
                        set --
                        if [ -s "$SAST_TEMP_DIR/github-authorization.header" ]; then
                            set -- --header "@$SAST_TEMP_DIR/github-authorization.header"
                        fi
                        echo 'Проверка размера ZIP по HTTP-заголовкам. Лимит загрузки: 100 МБ.'
                        head_status=$(curl --silent --show-error --location --max-redirs 5 \
                            --proto '=https' --proto-redir '=https' \
                            --connect-timeout 10 --max-time 30 --head \
                            --header 'Accept: application/vnd.github+json' \
                            --header 'X-GitHub-Api-Version: 2022-11-28' \
                            "$@" --dump-header "$SAST_TEMP_DIR/archive-headers.txt" \
                            --output /dev/null --write-out '%{http_code}' "$SAST_ARCHIVE_URL")
                        if [ "$head_status" != 200 ]; then
                            echo "Не удалось проверить ZIP: HTTP $head_status. Архив не скачивался." >&2
                            exit 1
                        fi
                        archive_hint=$(awk '
                            /^HTTP\\// {size=""}
                            tolower($1) == "content-length:" {gsub("\\r", "", $2); size=$2}
                            END {print size}
                        ' "$SAST_TEMP_DIR/archive-headers.txt")
                        if [ -n "$archive_hint" ]; then
                            echo "Заявленный размер ZIP: $archive_hint байт."
                            if [ "$archive_hint" -gt 104857600 ]; then
                                echo 'ZIP больше 100 МБ. Загрузка и сканирование не выполняются.' >&2
                                exit 1
                            fi
                        else
                            echo 'GitHub не сообщает размер ZIP заранее. Ограничение 100 МБ применяется во время передачи.'
                        fi
                        echo 'Скачивание ZIP потоком на диск, без распаковки. Дополнительные объекты Git LFS не запрашиваются.'
                        if status=$(curl --silent --show-error --location --max-redirs 5 \
                            --proto '=https' --proto-redir '=https' \
                            --connect-timeout 15 --max-time 180 --max-filesize 104857600 \
                            --header 'Accept: application/vnd.github+json' \
                            --header 'X-GitHub-Api-Version: 2022-11-28' \
                            "$@" --output "$SAST_TEMP_DIR/code.zip" \
                            --write-out '%{http_code}' "$SAST_ARCHIVE_URL"); then
                            if [ "$status" != 200 ]; then
                                echo "Не удалось скачать ZIP: HTTP $status." >&2
                                exit 1
                            fi
                        else
                            curl_exit=$?
                            rm -f "$SAST_TEMP_DIR/code.zip"
                            if [ "$curl_exit" -eq 63 ]; then
                                echo 'ZIP больше 100 МБ. Загрузка остановлена; сканирование не выполняется.' >&2
                            else
                                echo "Ошибка скачивания ZIP, код curl: $curl_exit." >&2
                            fi
                            exit 1
                        fi
                        archive_bytes=$(wc -c < "$SAST_TEMP_DIR/code.zip")
                        echo "ZIP-архив скачан: $archive_bytes байт."
                        if [ "$archive_bytes" -gt 26214400 ]; then
                            echo 'Размер ZIP превышает ограничение SAST API: 25 МБ.' >&2
                            exit 1
                        fi
                    ''')
                }
                echo "Источник SAST: ${env.SAST_REPOSITORY_URL}, ветка ${env.SAST_BRANCH}, коммит ${env.SAST_COMMIT}."
                withEnv(["SAST_PROJECT_NAME=${params.PROJECT_NAME}"]) {
                    echo 'Отправка ZIP в SAST. Ожидание ответа — до 5 минут.'
                    sh(label: 'POST /api/scans/archive', script: '''
                        set +x
                        umask 077
                        status=$(curl --silent --show-error \
                            --connect-timeout 15 --max-time 300 \
                            --header "@$SAST_TEMP_DIR/authorization.header" \
                            --form "archive=@$SAST_TEMP_DIR/code.zip;type=application/zip" \
                            --form-string "release=$SAST_RELEASE" \
                            --form-string "projectName=$SAST_PROJECT_NAME" \
                            --form-string "commitHash=$SAST_COMMIT" \
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
                    if (result.commitHash != env.SAST_COMMIT) {
                        error('Хеш коммита в ответе SAST не совпадает с коммитом проверенного ZIP.')
                    }
                    echo "Сканирование ${result.scanId}: файлов ${result.filesScanned}, найдено проблем ${result.summary.total}."
                    echo "Проверенный коммит: ${result.commitHash}."
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
