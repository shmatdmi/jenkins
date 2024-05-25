def jenkinsAgent = any
pipeline {
    agent {
        node {
            label "${jenkinsAgent}"
        }
    }
    options {
 //       timeout(time: 5, unit: 'HOURS')
 //       ansiColor('xterm')
 //       timestamps()
    }
    parameters {
        booleanParam(name: "dryrun", defaultValue: true, description: "Тестовый запуск")
        string(name: "version", defaultValue: "r48", trim: true, description: "Введите версию компонента")
        text(name: "releaseNotes", defaultValue: "Добавлены новые feature", description: "Описание изменений в релизе")
        password(name: "password", defaultValue: "changeme", description: "Введите пароль")
        choice(name: "env", choices: ["PROD", "DEV", "UAT"], description: "Sample multi-choice parameter")
    }
    stages {
        stage('DryRun') {
            when {
                expression { params.dryrun }
            }
            steps {
                echo "THIS IS DRYRUN!"
            }
        }
        stage("Build") {
            steps {
                echo "\033[32m==========================Build stage==========================\033[0m"
                echo "Build stage."
                echo "Hello $params.version"
            }
        }
        stage("Test") {
            steps {
                echo "Test stage."
                echo "Hello $params.version"
                sleep 10
                echo "$TAG_TIMESTAMP"
                sleep 5
                echo "$BUILD_DISPLAY_NAME"
            }
        }
        stage("Release") {
            steps {
                echo "Defined release notes $params.releaseNotes"
                echo "Starting release on $params.env"
            }
        }
    }
    post {

        // success {

        //     mail to: 'dvshmatov@sberbank.ru', cc: 'dvshmatov@omega.sbrf.ru', subject: '[SUCCESS] Оповещение о результатах сканирования', body: "Тестовое сканирование ${env.REPO_URL}, ветка ${env.BRANCH} завершилось успешно \"Hello\"\nworld"

        // }

        failure {

        mail to: 'ngt@live.ru', cc: 'shmatov787@gmail.ru', subject: '[CRITICAL] Оповещение о результатах сканирования', body: "Тестовое сканирование ${env.REPO_URL}, ветка ${env.BRANCH} завершилось неуспешно на этапе \"${env.FAILED_STAGE}\"\n${env.MESSAGE ?: ''}"

            }

        success {

            emailext to: 'ngt@live.ru',

                attachLog: true,

                attachmentsPattern: '*.txt',

                mimeType: 'text/html',

                recipientProviders: [requestor()],

                subject: "${JOB_BASE_NAME} - build:${BUILD_NUMBER} - successful",

                body: """

                    <div style= 'font-family:"Arial",sans-serif; background-color: #EEEEEE; border: 2px solid green;'>

                    <h3 style='background-color:green; color:white; font-size: 19px;'>Успешное выполнение джобы ${JOB_BASE_NAME}</h3>

                    <br>

                    <p>Название репозитория:${env.REPO_URL}</p>

                    <p>Лог сборки Jenkins во вложении</p>

                    <br>

                    </div>"""

            }            

        cleanup {

            cleanWs disableDeferredWipeout: true, deleteDirs: true

        }

    }
}