pipeline {
    agent any
//    agent {
//       node {
//            label 'any'
//        }
//    }
    options {
        timestamps()
        ansiColor('xterm')
    }
    parameters {
        booleanParam(name: "dryrun", defaultValue: false, description: "Тестовый запуск")
        booleanParam(name: "curl", defaultValue: true, description: "Запрос к сайту")
        booleanParam(name: "new_commit", defaultValue: true, description: "Создание нового коммита")
        string(name: "BRANCH_TO_SCAN", defaultValue: "main", trim: true, description: "Ветка для сканирования")
        choice(name: "env", choices: ["PROD", "DEV", "IFT"], description: "Sample multi-choice parameter")
    }
    stages {
        stage("Подготовка нового коммита для сканирования") {
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
                }
                script {
                    env.COMMIT_HASH = "${sh returnStdout: true, script: 'git rev-parse HEAD'}".trim()
                }
                echo "\033[32m==========\n\nNew commit hash: ${env.COMMIT_HASH}\n\n==========\033[0m"
            }
            post {
                failure {
                    script {
                        env.FAILED_STAGE = "Подготовка нового коммита для сканирования"
                    }
                }
            }
        }
        stage('DryRun') {
            when {
                expression {
                    return params.dryrun
                }
            }
            steps {
                echo "THIS IS DRYRUN!"
            }
        }
        stage ('curl') {
            when {
                expression {
                    return params.curl
                }
            }
            steps {
                echo "Test stage."
                sh 'curl --version'
                sh 'curl -k http://mskweather.ru'
            }
        }
        stage("Release") {
            steps {
                echo "Defined release notes $params.releaseNotes"
                echo "Starting release on $params.env"
            }
        }
        stage("Sleep") {
            steps {
            sh 'sleep 10'
            sh 'ls'
            sh 'ls'
            println 123 + 234
            println 234 * 345 * 500 * 2345 * 545223
            }
        }
    }
        post {     
            cleanup {
                cleanWs disableDeferredWipeout: true, deleteDirs: true
            }

    }
}
