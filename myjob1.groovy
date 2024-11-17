pipeline {
    agent any
    environment {
      APPLICATION_NAME="myapp"
      REPOSITORY_NAME="myrepo"
      FILENAME="${APPLICATION_NAME}-app.yaml"
      ALREADY_EXISTS="false"
    }
    options {
        timestamps()
        ansiColor('xterm')
    }
    parameters {
        booleanParam(name: 'curl', defaultValue: false, description: 'Запрос к сайту')
        booleanParam(name: 'new_commit', defaultValue: true, description: 'Создание нового коммита')
        booleanParam(name: 'git', defaultValue: true, description: 'git checkout on if else stage')
        booleanParam(name: 'if', defaultValue: true, description: 'if else stage')
        string(name: 'BRANCH_TO_SCAN', defaultValue: 'main', trim: true, description: 'Ветка для сканирования')
        choice(name: 'env', choices: ['PROD', 'DEV', 'IFT'], description: 'Sample multi-choice parameter')
    }

    stages {
        stage("Clone Git Repository") {
            when {
                expression {
                    return params.git
                }
            }
            steps {
                git(
                    url: "https://github.com/shmatdmi/jenkins.git",
                    branch: "main",
                    changelog: true,
                    //credentialsId: 'mycreds',
                    poll: true
                )
            }
        }
        stage ('Main Stage') {
            options {
                timeout(time: 1, unit: 'MINUTES')
            }
            when {
                expression {
                    return params.if
                }
            }
            sh 'ls -la'
            steps {
                sh '''
                  cd ./apps
                  if [ -f "${FILENAME}" ]; then
                    echo "${FILENAME} exists"
                    ALREADY_EXISTS="true"
                  else
                    echo "${FILENAME} does not exist"
                    ALREADY_EXISTS="false"
                  fi
                  echo "ALREADY_EXISTS = ${ALREADY_EXISTS}"
                  cd ..
                '''
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
            writeFile file: 'code.groovy', text: "echo '${new Date()} [${env.BUILD_NUMBER}]'\necho '${env.BRANCH_TO_SCAN} of ${env.GIT_URL}'\necho '${UUID.randomUUID().toString()}'"
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
        stage('curl') {
            when {
                expression {
                    return params.curl
                }
            }
            steps {
                sh 'curl -k http://mskweather.ru'
            }
        }
        stage('Release') {
            steps {
                echo "Starting release on $params.env"
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
