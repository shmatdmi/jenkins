pipeline {
    agent any
    environment {
      APPLICATION_NAME="myapp"
      REPOSITORY_NAME="myrepo"
      FILENAME="${APPLICATION_NAME}-weather.json"
      ALREADY_EXISTS="false"
    }
    triggers {
        cron('H 12 */3 * *')
    }
    options {
        timestamps()
        ansiColor('xterm')
    }
    parameters {
        booleanParam(name: 'new_commit', defaultValue: true, description: 'Создание нового коммита')
        booleanParam(name: 'if', defaultValue: true, description: 'if else stage')
        string(name: 'BRANCH_TO_SCAN', defaultValue: 'main', trim: true, description: 'Ветка для сканирования')
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
                    sh "cd ./data"
                    sh "curl -m 2 'https://api.openweathermap.org/data/2.5/weather?q=Moscow,RU&appid=ba23e3e7888484e7a26b57b215d65200&units=metric' > ./data/${APPLICATION_NAME}-weather.json"
                    sh "ls -la ./data"
                    // Предполагаем, что json хранится в файле
                    def jsonContent = readFile(file: './data/myapp-weather.json')
                    
                    // Парсим JSON в объект
                    def data = readJSON text: jsonContent
                    
                    echo "Temp: ${data.main.temp}"
                    echo "Wind: ${data.wind.speed}"
                    echo "City: ${data.name}"
                    echo "Weather: ${data.weather.join(', ')}"
                // if-else
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
        stage('Подготовка нового коммита для сканирования') {
            options {
                timeout(time: 1, unit: 'MINUTES')
            }
            when {
                expression {
                    return params.new_commit
                }
            }
        
            steps {
            echo "\033[32m==========================New commit stage==========================\033[0m"
            sshagent(['ssh-dima']) {
            sh "git checkout ${env.BRANCH_TO_SCAN}"
            //writeFile file: 'code.groovy', text: "echo '${new Date()} [${env.BUILD_NUMBER}]'\necho '${env.BRANCH_TO_SCAN} of ${env.GIT_URL}'\necho '${UUID.randomUUID().toString()}'"
            writeFile file: 'code.groovy', text: "cat '[${env.FILENAME}]'\necho '${env.BRANCH_TO_SCAN} of ${env.GIT_URL}'\necho '${UUID.randomUUID().toString()}'"         
            sh 'git add code.groovy'
            sh "git commit -am \"Auto #${env.BUILD_NUMBER}\""
            sh "git push origin ${env.BRANCH_TO_SCAN}:${env.BRANCH_TO_SCAN}"
            }
                script {
                    env.COMMIT_HASH = "${sh returnStdout: true, script: 'git rev-parse HEAD'}".trim()
                }
                echo "\033[32m==========\n\nNew commit hash: ${env.COMMIT_HASH}\n\n==========\033[0m"
            }
            post {
                failure {
                    script {
                        env.FAILED_STAGE = 'Подготовка нового коммита для сканирования'
                    }
                }
            }
        }
        stage('Release') {
            steps {
                echo "Starting release on $params.BRANCH_TO_SCAN branch" // пример вывода параметра
                echo "Environment example: $env.REPOSITORY_NAME"
            }
        }
    }
    post {
        cleanup {
                cleanWs disableDeferredWipeout: true, deleteDirs: true
        }
        failure {
            mail to: 'shmatov787@gmail.com',
            subject: "Failure project - Jenkins Pipeline: ${currentBuild.fullDisplayName}",
            body: "Your build FAILED, please check: ${env.BUILD_URL}"
        }
    }
}
