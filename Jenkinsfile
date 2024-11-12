pipeline {
    agent none
    environment {
        javaVersion = '/usr/var/java11'
    }
    options {
        timestamps()
        ansiColor('xterm')
    }
    stages {
        stage('Examle username password') {
            agent any
            environment {
                SERVICE_CRED = credentials('github_cred')
            }
            steps {
                /* Masking supported pattern matches
                    of $SERVICE_CREDS
                    or $SERVICE_CREDS_USR
                    or $SERVICE_CREDS_PSW */
                echo "Service user is $SERVICE_CREDS_USR"
            }
        }
        stage('Build') {
            agent any
            steps {
                echo "build ${env.BUILD_ID} on ${env.JENKINS_URL}"
                echo "This is path ${env.javaVersion}"
                echo "This is path $javaVersion"
                sh 'printenv'
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