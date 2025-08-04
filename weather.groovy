pipeline {
    agent any
    triggers {
        cron('H 12 */3 * *')
    }
    environment {
      APPLICATION_NAME="msk"
      CURRENT_TEMP="1"
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
                    echo "\033[33m Waiting... \033[0m"
                    sleep 2
                    sh "rm -rf ./data"
                    sh "mkdir ./data"
                    sh "cd ./data"
                    sh "curl 'https://api.openweathermap.org/data/2.5/weather?q=Moscow,RU&appid=ba23e3e7888484e7a26b57b215d65200&units=metric' > ./data/${APPLICATION_NAME}-weather.json"
                    // Предполагаем, что json хранится в файле
                    def jsonContent = readFile(file: './data/msk-weather.json')
                    // Парсим JSON в объект
                    def data = readJSON text: jsonContent
                    echo "Temp: ${data.main.temp}"
                    echo "Wind: ${data.wind.speed}"
                    echo "City: ${data.name}"
                    echo "Weather: ${data.weather.join(', ')}"
                    def test = """${data.main.temp}
"""
                    echo "Local variable: ${test}"
                }
            }
        }
        stage('Use global variable') {
            steps {
                echo "Global variable: ${env.CURRENT_TEMP}"
            }
        }
    }
    post {
        cleanup {
                cleanWs disableDeferredWipeout: true, deleteDirs: true
        }
    }
}
