pipeline {
    agent none
    options {
        timestamps()
        ansiColor('xterm')
    }
    stages {
        stage('Build') {
            agent any
            steps {
                echo 'build'
            }
        }
        stage('Test') {
            agent any
            steps {
                echo 'test'
            }
        }
        stage('Deploy') {
            agent any
            steps {
                echo "\033[32m==========================Deploy stage==========================\033[0m"
                echo 'deploy'
                sh 'ls -la'
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