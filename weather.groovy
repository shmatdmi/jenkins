pipeline {
    agent any
    triggers {
        cron('H 7-23 */2 * *')
    }
    environment {
      APPLICATION_NAME="msk"
      MAIL="sberlinux@ya.ru"
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
                    def response = sh(script: "curl -v 'https://api.openweathermap.org/data/2.5/weather?q=Moscow,RU&appid=ba23e3e7888484e7a26b57b215d65200&units=metric'", returnStdout: true).trim()
                    echo "Response from server: ${response}"
                    sh "rm -rf ./data"
                    sh "mkdir ./data"
                    sh "cd ./data"
                    sh "curl 'https://api.openweathermap.org/data/2.5/weather?q=Moscow,RU&appid=ba23e3e7888484e7a26b57b215d65200&units=metric' > ./data/${APPLICATION_NAME}-weather.json"
                    // Предполагаем, что json хранится в файле
                    def jsonContent = readFile(file: "./data/${APPLICATION_NAME}-weather.json")
                    // Парсим JSON в объект
                    def data = readJSON text: jsonContent
                    echo "Temp: ${data.main.temp}"
                    echo "Wind: ${data.wind.speed}"
                    echo "City: ${data.name}"
                    echo "Weather: ${data.weather.join(', ')}"
                    env.TEMP = "${data.main.temp}"
                    env.WIND = "${data.wind.speed}"                    
                    echo "Global variable: ${env.TEMP}"
                }
            }
        }
        stage('Use global variable') {
            steps {
                    echo "Global variable: ${env.TEMP}"
                    echo "Global variable: ${env.WIND}"
            }
        }
    }
    post {
        cleanup {
                cleanWs disableDeferredWipeout: true, deleteDirs: true
        }
        success {
            mail to: "${env.MAIL}",
            subject: "Погода в Москве сейчас",
            body: """Температура: ${env.TEMP}, Скорость ветра: ${env.WIND}"""
            echo 'Im success'
        }
        failure {
            mail to: "${env.MAIL}",
            subject: "Failure project - Jenkins Pipeline: ${currentBuild.fullDisplayName}",
            body: "Failure project - Jenkins Pipeline: ${currentBuild.fullDisplayName}"
            echo 'Im failed'
        }
    }
}
