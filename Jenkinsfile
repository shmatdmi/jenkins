pipeline {
    agent any
    triggers {
        cron('H */10 * * 1-5')
    }
    parameters {
        string(name: 'FIRST_NAME', defaultValue: 'Ivan',
                description: 'This is your name')
        string(name: 'LAST_NAME', defaultValue: 'Ivanov',
                description: '')
        text(name: 'MESSAGE', defaultValue: '',
                description: 'Enter some information about the news')
        booleanParam(name: 'DO_IT', defaultValue: true,
                description: '.....')
        choice(name: 'CHOICE', choices: ['one', '2', 'Three'],
                description: 'Pick something')
        password(name: 'PASSWORD', defaultValue: 'SECRET',
                description: 'Enter a password')
    }
    environment {
        javaVersion = '/usr/var/java11'
    }
    options {
        timestamps()
        ansiColor('xterm')
        timeout(time: 1, unit: 'MINUTES')
    }
    stages {
        stage('Examle username password') {
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
            }
        }
        stage('Build') {
            steps {
                echo "\033[31m==========================Envirenments==========================\033[0m"
                echo "build ${env.BUILD_ID} on ${env.JENKINS_URL}"
                echo "This is path ${env.javaVersion}"
                echo "This is path $javaVersion"
                sh 'printenv'
                sleep 3
            }
        }
    }
    post {
        cleanup {
                cleanWs disableDeferredWipeout: true, deleteDirs: true
            }
        success {
            mail to: 'shmatov787@gmail.com',
            subject: "Completed Pipeline: ${currentBuild.fullDisplayName}",
            body: "Your build completed, please check: ${env.BUILD_URL}"
            echo 'Im successed'
        }
        failure {
            mail to: 'shmatov787@gmail.com',
            subject: "Failure project - Jenkins Pipeline: ${currentBuild.fullDisplayName}",
            body: "Your build FAILED, please check: ${env.BUILD_URL}"
            echo 'Im failed'
        }
    }   
}