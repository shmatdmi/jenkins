pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                echo 'Hello World'
                println ${JOB_NAME}
            }
        }
    }
}
