pipeline {
    agent any
    triggers {
        cron('H 12 */3 * *')
    }

    parameters {
        string(name: 'FIRST_NAME', defaultValue: 'Dima', description: 'This is your name')
        //text(name: 'MESSAGE', defaultValue: '', description: 'Enter some information about the news')
        password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
        booleanParam(name: 'new_commit', defaultValue: true, description: 'Создание нового коммита')
        booleanParam(name: 'if', defaultValue: true, description: 'if else')
        string(name: 'BRANCH_TO_SCAN', defaultValue: 'main', trim: true, description: 'Ветка для сканирования')
        choice(name: 'env', choices: ['PROD', 'DEV', 'IFT'], description: 'Sample multi-choice parameter')
    }

    environment {
        javaVersion = '/usr/var/java11'
        name = 'Dima'
        sity = 'Moscow'
        login = 'shmatov787'
    }

    options {
        timestamps()
        ansiColor('xterm')
        timeout(time: 1, unit: 'MINUTES') //таймаут
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

        stage('Example username password') {
            agent any
            environment {
                SERVICE_CRED = credentials('github_cred')
            }
            steps {
                echo "\033[32m==========================Parameters==========================\033[0m"
                echo "Name ${params.FIRST_NAME}"
                echo "Password: ${params.PASSWORD}"
            }
        }
        stage ('if') {
            options {
                timeout(time: 1, unit: 'MINUTES')
            }
            when {
                expression {
                    return params.if
                }
            }
            steps {
                script {
                    if (params.env == 'PROD') {
                        echo 'Deploy on prodaction'
                    } else {
                        echo 'Deploy on development'
                    }
                }
            }
        }
        stage('Add env on steps') {
            agent any
            options {
                timeout(time: 1, unit: 'MINUTES')
            }
            steps {
                echo "\033[35m========================Envirenments====================\033[0m"
                script {
                    withEnv(["name=Max"]) {
                        echo "${env.name}"
                        def quality = 'superhero'
                        def test = "DevOps - is ${quality}"
                        echo "${test}"
                        env.DATABASE = "sast"
                        echo "${env.DATABASE}"
                        name = "Dima"
                        echo "$name"

                echo ${env.DATABASE}
                        x = 3
                        println x * 3
                        int count = 5
                        echo "$count"
                    }
                }
                echo "\033[32m========================Global envirenments====================\033[0m"
                echo "build ${env.BUILD_ID} on ${env.JENKINS_URL}"
                echo "This is path ${env.javaVersion}"
                echo "This is path $javaVersion"
                echo "\033[32m$sity\033[0m"
                echo "${env.DATABASE}"
                sh 'printenv'
            }
        }
    }
    post {
        cleanup {
                cleanWs disableDeferredWipeout: true, deleteDirs: true
            }
        success {
            mail to: "${env.login}@gmail.com",
            subject: "Completed Pipeline: ${currentBuild.fullDisplayName}",
            body: "Your build completed, please check: ${env.BUILD_URL}"
            echo 'Im successed'
        }
        failure {
            mail to: "${env.login}@gmail.com",
            subject: "Failure project - Jenkins Pipeline: ${currentBuild.fullDisplayName}",
            body: "Your build FAILED, please check: ${env.BUILD_URL}"
            echo 'Im failed'
        }
    }   
}