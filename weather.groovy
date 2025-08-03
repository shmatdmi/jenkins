pipeline {
    agent any
    triggers {
        cron('H 12 */3 * *')
    }
    options {
        timestamps()
        ansiColor('xterm')
    }
    parameters {
        string(name: 'BRANCH_TO_SCAN', defaultValue: 'main', trim: true, description: 'Ветка для сканирования')
        choice(name: 'env', choices: ['PROD', 'DEV', 'IFT'], description: 'Sample multi-choice parameter')
    }

    stages {
        stage ('curl') {
            steps {
                echo "\033[32m==========================curl==========================\033[0m"
                script {
                def response = sh(script: 'curl -m 2 https://api.openweathermap.org/data/2.5/weather?q=Moscow,RU&appid=ba23e3e7888484e7a26b57b215d65200&units=metric', returnStdout: true).trim()
                echo "waiting"
                sh sleep 3
                echo "Response from server: ${response}"
                }
            }
        }
    }
    post {
        cleanup {
                cleanWs disableDeferredWipeout: true, deleteDirs: true
        }
    }
}
