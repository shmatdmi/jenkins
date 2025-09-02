pipeline {
    agent any // Выполняем на любом доступном узле

    parameters { // Список параметров, задаваемых вручную
        choice(name: 'ENVIRONMENT', choices: ['dev', 'test', 'prod'], description: 'Выберите окружение')
    }

    stages {
        stage('prod') {
            when {
                expression { return params.ENVIRONMENT == 'prod' } // Запускаем сборку только для dev/test
            }
            steps {
                echo "choise: prod"
            }
        }
        
        stage('test') {
            when {
                expression { return params.ENVIRONMENT == 'test' } // Запускаем сборку только для dev/test
            }
            steps {
                echo "choise: test"
            }
        }
        
        stage('dev') {
            when {
                expression { return params.ENVIRONMENT == 'dev' } // Запускаем сборку только для dev/test
            }
            steps {
                echo "choise: dev"
            }
        }
    }
    
    post { // Действия после завершения всех стадий
        always {
            cleanWs()
        }
    }
}