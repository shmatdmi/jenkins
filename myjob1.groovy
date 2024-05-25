def jenkinsAgent = any
pipeline {
    agent any
//    agent {
//        node {
//            label "${jenkinsAgent}"
//        }
//    }
//    options {
//        timeout(time: 5, unit: 'HOURS')
//        ansiColor('xterm')
//        timestamps()
//    }
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
                echo "$TAG_TIMESTAMP"
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
        cleanup {

            cleanWs disableDeferredWipeout: true, deleteDirs: true

        }

    }
}