pipeline {
    agent any
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
                echo "Build stage."
                echo "Hello $params.version"
            }
        }
        stage("Test") {
            steps {
                echo "Test stage."
                echo "Hello $params.version"
            }
        }
        stage("Release") {
            steps {
                echo "Defined release notes $params.releaseNotes"
                echo "Starting release on $params.env"
            }
        }
    }
}