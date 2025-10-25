dima = 123

pipeline {
    agent any
    
    environment {
        APPLICATION_NAME="msk"
        MAIL="sberlinux@ya.ru"
        POSTGRES_HOST     = credentials('postgres_host')
        POSTGRES_PORT     = '5432'
        POSTGRES_DBNAME   = 'postgres_db'
        POSTGRES_USERNAME = credentials('postgres_user')
        POSTGRES_PASSWORD = credentials('postgres_password')
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
        stage ('weather curl') {
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
                    echo "Weather: ${data['weather'][0]['main']}"
                    env.TEMP = "${data.main.temp}"
                    env.WIND = "${data.wind.speed}"
                    env.CITY = "${data.name}"
                    env.META_DATA = "${data.weather.join(', ')}"
                    env.MAIN = "${data['weather'][0]['main']}"               
                    echo "Global variable: ${env.MAIN}"
                    dima = '12345' //пример работы с переменными, эта доступна только в текущем шаге, для её переноа в следующий шаг, нужно объявить её заранее, в самом начале pipeline
                    env.TEST = "${dima}" //
                    echo "def: ${dima}"
                    echo "env: ${env.TEST}"
                    
                }
            }
        }
        stage('if') {
            steps {
                echo "def_preview_stage: ${dima}"
                echo "env_preview_stage: ${env.TEST}" //использую переменную из предидущего шага
                script {
                    if (env.MAIN == 'Clear') {
                        env.MAIN_POST = "Ясно"
                    } else if (env.MAIN == 'Rain') {
                        env.MAIN_POST = "Дождь"
                    } else if (env.MAIN == 'Clouds') {
                        env.MAIN_POST = "Облачно"
                    } else {
                        env.MAIN_POST = "Не определено"
                    }
                }
            }
        }
        stage('echo env') {
            steps {
                echo "Main post env: ${MAIN_POST}"
            }
        }
    }
    post {
        cleanup {
                cleanWs disableDeferredWipeout: true, deleteDirs: true
        }
        failure {
            mail to: "${env.MAIL}",
            subject: "Failure project - Jenkins Pipeline: ${currentBuild.fullDisplayName}",
            body: "Failure project - Jenkins Pipeline: ${currentBuild.fullDisplayName}"
            echo 'Im failed'
        }
    }
}