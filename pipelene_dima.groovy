pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                echo 'Hello World'
                println ${JOB_NAME}
                echo "Another method is to use \${BUILD_NUMBER}, which is ${BUILD_NUMBER}"
            }
        }
    }
}
