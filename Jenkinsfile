pipeline {
    agent none
    options {
        timestamps()
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
                echo 'deploy'
                sh 'ls -la'
            }
        }
    }   
}