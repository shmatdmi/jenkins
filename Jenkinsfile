pipeline {
    agent none
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
            }
        }
        stage('Build') {
            agent any
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
        success {
           echo 'Im successed'
        }
        failure {
           echo 'Im failed'
        }        
        aborted {
           echo 'Project aborted'
        }
    }   
}