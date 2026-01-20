pipeline {
    agent any
    triggers {
        cron('0 7-23/5 * * *')
    }
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
        booleanParam(name: 'write_db', defaultValue: true, description: 'Запись в БД')
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
                    echo "\033[32m==========================json==========================\033[0m"
                    echo "${jsonContent}"
                    // Парсим JSON в объект
                    def data = readJSON text: jsonContent
                    echo "\033[32m==========================Map==========================\033[0m"
                    echo "${data}"
                    echo "Temp: ${data.main.temp}"
                    echo "Wind: ${data.wind.speed}"
                    echo "City: ${data.name}"
                    echo "Weather: ${data.weather.join(', ')}"
                    echo "Main: ${data['weather'][0]['main']}"
                    env.TEMP = "${data.main.temp}"
                    env.WIND = "${data.wind.speed}"
                    env.CITY = "${data.name}"
                    env.META_DATA = "${data.weather.join(', ')}" 
                    env.MAIN = "${data['weather'][0]['main']}"             
                    echo "Global variable: ${env.TEMP}"                    
                }
            }
        }
        stage('if') {
            steps {
                script {
                    if (env.MAIN == 'Clear') {
                        env.MAIN_POST = "Ясно"
                    } else if (env.MAIN == 'Rain') {
                        env.MAIN_POST = "Дождь"
                    } else if (env.MAIN == 'Snow') {
                        env.MAIN_POST = "Cнег"                        
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
        stage('Database Update') {
            when {
                expression {
                    return params.write_db
                }
            }
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'postgres-user-pass',
                        usernameVariable: 'DB_USER',
                        passwordVariable: 'DB_PASS'
                    )]) {
                        sh """
                            PGPASSWORD=\"\$DB_PASS\" psql -h ${POSTGRES_HOST} -p ${POSTGRES_PORT} -U \"\$DB_USER\" -d ${POSTGRES_DBNAME} -w <<EOF
                            insert into public.stat_weather(temperature, wind_speed, city, meta_data) values ('${env.TEMP}', ${env.WIND}, '${env.CITY}', '${env.META_DATA}');
                            EOF
                        """
                    }
                }
            }
        }
    }
    post {
        cleanup {
                cleanWs()
        }
        success {
            mail to: "${env.MAIL}",
            subject: "Погода в Москве",
            body: """Температура: ${env.TEMP}, Скорость ветра: ${env.WIND}, На улице: ${env.MAIN_POST}"""
        }
        failure {
            mail to: "${env.MAIL}",
            subject: "Failure project - Jenkins Pipeline: ${currentBuild.fullDisplayName}",
            body: "Failure project - Jenkins Pipeline: ${currentBuild.fullDisplayName}"
            echo 'Im failed'
        }
    }
}