pipeline {
    agent any // Выполняем на любом доступном узле

    parameters { // Список параметров, задаваемых вручную
        choice(name: 'ENVIRONMENT', choices: ['dev', 'test', 'prod'], description: 'Выберите окружение')
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Запустить тесты?')
    }

    stages {
        stage('Подготовка') {
            steps {
                echo "Подготовительный этап..."
                script {
                    if ("${params.RUN_TESTS}") {
                        echo 'yes' // Установка зависимостей
                    }
                }
            }
        }
        
        stage('Сборка') {
            when {
                expression { return params.ENVIRONMENT == 'prod' } // Запускаем сборку только для dev/test
            }
            steps {
                echo "choise: prod"
            }
        }
        
        stage('Тестирование') {
            when {
                expression { return "${params.RUN_TESTS}" && params.ENVIRONMENT == 'test' } // Тестируем только тестовое окружение
            }
            steps {
                echo "2 params"
            }
        }
        
        stage('Развертывание') {
            when {
                expression { return params.ENVIRONMENT == 'test' } // Развёртывание выполняется только в продакшене
            }
            steps {
                echo "choise: test"
            }
        }
    }
    
    post { // Действия после завершения всех стадий
        always {
            cleanWs()
        }
    }
}