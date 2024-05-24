pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                echo 'Hello World'
                println "${GIT_COMMIT}"
                echo "Another method is to use \${BUILD_NUMBER}, which is ${BUILD_NUMBER}"
            }
        }
    }
}
