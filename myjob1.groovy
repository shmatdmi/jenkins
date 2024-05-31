pipeline {
    agent any
//    agent {
//       node {
//            label 'any'
//        }
//    }
    parameters {
        booleanParam(name: "dryrun", defaultValue: true, description: "Тестовый запуск")
        string(name: "version", defaultValue: "r48", trim: true, description: "Введите версию компонента")
        text(name: "releaseNotes", defaultValue: "Добавлены новые feature", description: "Описание изменений в релизе")
        password(name: "password", defaultValue: "changeme", description: "Введите пароль")
        choice(name: "env", choices: ["PROD", "DEV", "UAT"], description: "Sample multi-choice parameter")
//        string(name: "web", defaultValue "http://mskweather.ru", trim: true, description: "Введите адрес сайта")
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
                sh 'curl --version'
//                sh 'curl -X -k POST http://mskweather.ru/'
//                echo "$params.web"
                sh 'curl -v -k http://mskweather.ru'
            }
        }
        stage("Release") {
            steps {
                echo "Defined release notes $params.releaseNotes"
                echo "Starting release on $params.env"
            }
        }
        stage("Sleep") {
            steps {

            sh 'sleep 120'
            }
        }
    }
        post {     
            cleanup {
                cleanWs disableDeferredWipeout: true, deleteDirs: true
            }

    }
}