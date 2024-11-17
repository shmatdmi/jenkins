pipeline {
    agent any
    triggers {
        cron('H */3 * * *')
    }
    parameters {
        string(name: 'FIRST_NAME', defaultValue: 'Ivan', description: 'This is your name')
        string(name: 'LAST_NAME', defaultValue: 'Ivanov', description: '')
        text(name: 'MESSAGE', defaultValue: '', description: 'Enter some information about the news')
        booleanParam(name: 'DO_IT', defaultValue: true, description: '.....')
        choice(name: 'CHOICE', choices: ['one', '2', 'Three'], description: 'Pick something')
        password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
        booleanParam(name: 'dryrun', defaultValue: false, description: 'Тестовый запуск')
        booleanParam(name: 'curl', defaultValue: true, description: 'Запрос к сайту')
        booleanParam(name: 'new_commit', defaultValue: true, description: 'Создание нового коммита')
        string(name: 'BRANCH_TO_SCAN', defaultValue: 'main', trim: true, description: 'Ветка для сканирования')
        choice(name: 'env', choices: ['PROD', 'DEV', 'IFT'], description: 'Sample multi-choice parameter')
    }
    environment {
        javaVersion = '/usr/var/java11'
        name = 'Dima'
        sity = 'Moscow'
    }
    options {
        timestamps()
        ansiColor('xterm')
        timeout(time: 2, unit: 'MINUTES') //таймаут
    }
    stages {
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
            sh 'git config --global user.email "dima@example.com"'
            sh 'git config --global user.name "Dima"'
            sh "git commit -am \"Auto #${env.BUILD_NUMBER}\""
            sh "git push origin ${env.BRANCH_TO_SCAN}:${env.BRANCH_TO_SCAN}"
            sleep 60
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

        stage('Examle username password') {
            agent any
            environment {
                SERVICE_CRED = credentials('github_cred')
            }
            steps {
                echo "\033[32m==========================Parameters==========================\033[0m"
                echo "Hello ${params.FIRST_NAME}"
                echo "Biography: ${params.LAST_NAME}"
                echo "Toggle: ${params.DO_IT}"
                echo "Choice: ${params.CHOICE}"
                echo "Password: ${params.PASSWORD}"
                echo "$name"
                echo "\033[32m$sity\033[0m"
                sleep 5
            }
        }
        stage('Build') {
            agent any
            options {
                timeout(time: 1, unit: 'MINUTES')
            }
            steps {
                echo "\033[31m==========================Envirenments==========================\033[0m"
                echo "build ${env.BUILD_ID} on ${env.JENKINS_URL}"
                echo "This is path ${env.javaVersion}"
                echo "This is path $javaVersion"
                echo "$sity"
                sh 'printenv'
                sleep 5
            }
        }
    }
    post {
        cleanup {
                cleanWs disableDeferredWipeout: true, deleteDirs: true
            }
        /*success {
            mail to: 'shmatov787@gmail.com',
            subject: "Completed Pipeline: ${currentBuild.fullDisplayName}",
            body: "Your build completed, please check: ${env.BUILD_URL}"
            echo 'Im successed'
        }*/
        failure {
            mail to: 'shmatov787@gmail.com',
            subject: "Failure project - Jenkins Pipeline: ${currentBuild.fullDisplayName}",
            body: "Your build FAILED, please check: ${env.BUILD_URL}"
            echo 'Im failed'
        }
    }   
}