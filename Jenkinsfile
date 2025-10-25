pipeline {
    agent any
    triggers {
        cron('H 12 */5 * *')
    }

    parameters {
        text(name: 'MESSAGE', defaultValue: '', description: 'Enter some information about the news')
        password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
        booleanParam(name: 'new_commit', defaultValue: true, description: 'Создание нового коммита')
        booleanParam(name: 'if', defaultValue: true, description: 'if else')
        booleanParam(name: 'env', defaultValue: true, description: 'env')       
        string(name: 'BRANCH_TO_SCAN', defaultValue: 'main', trim: true, description: 'Ветка для сканирования')
        choice(name: 'stand', choices: ['PROD', 'DEV', 'IFT'], description: 'Sample multi-choice parameter')
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
        timeout(time: 1, unit: 'MINUTES') //таймаут для всех stage
    }

    stages {
        stage('Подготовка нового коммита для сканирования') {
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
        stage ('if') {
            options {
                timeout(time: 1, unit: 'MINUTES') //таймаут выполнения этого шага
            }
            when {
                expression {
                    return params.if //шаг проверяет нужно ли выполняться, задаем это при сборке с параметрами
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
        stage('Env') {
            when {
                expression {
                    return params.env
                }
            }
           /* environment {
                SERVICE_CRED = credentials('github_cred') //добавил возможность использовать данные из cred jenkins
            } */
            steps {
                echo "\033[35m========================Envirenments====================\033[0m"
                script {
                    withEnv(["name=Max"]) {
                        echo "${env.name}"
                        def quality = 'superhero'  //add env
                        def test = "DevOps - is ${quality}" //add env
                        echo "${test}"
                        env.DATABASE = "sast" //add global env
                        echo "${env.DATABASE}"
                        name = "Dima"
                        echo "$name"
                        x = 3 //add env
                        println x * 3 //вывести результат умножения и перенести строку
                        int count = 5 //добавить числовую переменную
                        echo "$count"
                        echo "Starting release on $params.BRANCH_TO_SCAN branch" // пример вывода параметра
                    }
                }
                echo "\033[32m========================Global envirenments====================\033[0m"
                echo "build ${env.BUILD_ID} on ${env.JENKINS_URL}" //
                echo "This is path ${env.javaVersion}"
                echo "This is path $javaVersion"
                echo "\033[32m$sity\033[0m"
                echo "$params.MESSAGE"
                echo "\033[31m========================Workspace====================\033[0m"                
                echo "Workspace $WORKSPACE"
                echo "${env.DATABASE}" //
                sh 'printenv' //
            }
        }
    }
    post {
        cleanup {
                cleanWs disableDeferredWipeout: true, deleteDirs: true
            }
        success {
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