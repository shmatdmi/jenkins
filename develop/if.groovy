pipeline {
    agent any
    
    environment {
      APPLICATION_NAME="myapp"
      REPOSITORY_NAME="myrepo"
      FILENAME="${APPLICATION_NAME}-weather.json"
      ALREADY_EXISTS="false"
      MAIL="sberlinux@ya.ru"
    }
    triggers {
        cron('H 12 */3 * *')
    }
    options {
        timestamps()
        ansiColor('xterm')
    }
    parameters {
        booleanParam(name: 'if', defaultValue: true, description: 'if else stage')
        string(name: 'BRANCH_TO_SCAN', defaultValue: 'educations', trim: true, description: 'Ветка для сканирования')
        choice(name: 'env', choices: ['PROD', 'DEV', 'IFT'], description: 'Sample multi-choice parameter')
    }

    stages {
        stage ('if else') {
            options {
                timeout(time: 1, unit: 'MINUTES')
            }
            when {
                expression {
                    return params.if
                }
            }
            steps {
                echo "\033[32m==========================if else stage==========================\033[0m"
                script {
                    sh "cd ./data"
                    sh "curl -m 2 'https://api.openweathermap.org/data/2.5/weather?q=Moscow,RU&appid=ba23e3e7888484e7a26b57b215d65200&units=metric' > ./data/${APPLICATION_NAME}-weather.json"
                    sh '''
                    cd ./data
                    if [ -f "${FILENAME}" ]; then
                        echo "${FILENAME} exists"
                        exit 0
                        ALREADY_EXISTS="true"
                    else
                        echo "${FILENAME} does not exist"
                        ALREADY_EXISTS="false"
                        exit 1
                    fi
                    echo "ALREADY_EXISTS = ${ALREADY_EXISTS}"
                    cd ..
                    ''' // exit переводит сборку в failed
                }
            }
        }
        stage('Parsing json') {
            steps {
                script {
                    // Предполагаем, что json хранится в файле
                    def jsonContent = readFile(file: "./data/${APPLICATION_NAME}-weather.json")
                    // Парсим JSON в объект
                    def data = readJSON text: jsonContent
                    echo "Temp: ${data.main.temp}"
                    echo "Wind: ${data.wind.speed}"
                    echo "City: ${data.name}"
                    echo "Weather: ${data.weather.join(', ')}"
                    env.TEMP = "${data.main.temp}"
                }
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
            body: "Your build FAILED, please check: ${env.BUILD_URL}"
        }
    }
}
