pipeline {
    agent any
    environment {
      APPLICATION_NAME="myapp"
      REPOSITORY_NAME="myrepo"
      FILENAME="${APPLICATION_NAME}-app.yaml"
      ALREADY_EXISTS="false"
    }
    stages {
        stage("Clone Git Repository") {
            steps {
                git(
                    url: "https://github.com/shmatdmi/jenkins.git",
                    branch: "main",
                    changelog: true,
                    credentialsId: 'mycreds',
                    poll: true
                )
            }
        }
        stage("Check if file already exists") {
            steps {
                sh '''
                  cd ./apps
                  if [ -f "${FILENAME}" ]; then
                    echo "${FILENAME} exists"
                    ALREADY_EXISTS="true"
                  else
                    echo "${FILENAME} does not exist"
                    ALREADY_EXISTS="false"
                  fi
                  echo "ALREADY_EXISTS = ${ALREADY_EXISTS}"
                  cd ..
                '''
            }
        }
    }
}