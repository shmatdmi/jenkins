pipeline {
    agent 'Jenkins'
//    agent {
//       node {
//            label 'any'
//        }
//    }
    parameters {
        booleanParam(name: "dryrun", defaultValue: true, description: "Тестовый запуск")
        booleanParam(name: "curl", defaultValue: true, description: "Запрос к сайту")
        string(name: "version", defaultValue: "r48", trim: true, description: "Введите версию компонента")
        password(name: "password", defaultValue: "changeme", description: "Введите пароль")
        choice(name: "env", choices: ["PROD", "DEV", "UAT"], description: "Sample multi-choice parameter")
    }
    stages {
        stage('DryRun') {
            when {
                expression {
                    return params.dryrun
                }
            }
            steps {
                echo "THIS IS DRYRUN!"
            }
        }
        stage ('curl') {
            when {
                expression {
                    return params.curl
                }
            }
            steps {
                echo "Test stage."
                sh 'curl --version'
                sh 'curl -v -k http://mskweather.ru'
            }
        }
        stage("Build") {
            steps {
                echo "Build stage."
                echo "Hello $params.version"
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
            sh 'sleep 10'
            sh 'ls'
            sh 'rm -rf R*'
            sh 'ls'
            println 123 + 234
            println 234 * 345 * 500 * 2345 * 545223
            }
        }
    }
        post {     
            cleanup {
                cleanWs disableDeferredWipeout: true, deleteDirs: true
            }

    }
}