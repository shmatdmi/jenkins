pipeline {
    agent any

    environment {
        POSTGRES_HOST     = credentials('postgres_host')
        POSTGRES_PORT     = '5432'
        POSTGRES_DBNAME   = 'postgres_db'
        POSTGRES_USERNAME = credentials('postgres_user')
        POSTGRES_PASSWORD = credentials('postgres_password')
    }

    options {
        timestamps()
        ansiColor('xterm')
    }

    parameters {
        choice(name: 'TYPE', choices: ['Отжимания', 'Подтягивания', 'Пресс','Приседания', 'Бег'], description: 'Выберите вид упражений')
    }

    stages {
        stage('Database Update') {
            steps {
                script {
                    // Получаем результат SQL-запроса и сохраняем в переменную env.SUM_RESULT
                    withCredentials([usernamePassword(credentialsId: 'postgres-user-pass', usernameVariable: 'DB_USER', passwordVariable: 'DB_PASS')]) {
                        sh '''
                            export SUM_RESULT=$(
                                PGPASSWORD="\$DB_PASS" psql -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" -U "\$DB_USER" -d "${POSTGRES_DBNAME}" -w <<EOF
                                    select SUM(count_sport) from public.stat_sport where type_sport = '${params.TYPE}' AND create_date >= NOW() - INTERVAL '7 days';
                                EOF
                            )
                        '''
                    }
                    
                    echo "Сумма count_sport за последнюю неделю: \$SUM_RESULT"
                }
            }
        }
        
        stage('Следующий этап') {
            steps {
                script {
                    // Используем ранее сохранённую переменную
                    if ("${env.SUM_RESULT}".trim()) {
                        echo "Полученная сумма из предыдущего шага: ${env.SUM_RESULT}"
                        
                        // Здесь можешь продолжить работу с полученной суммой,
                        // например отправить уведомление или выполнить дополнительные шаги
                    } else {
                        error("Ошибка при выполнении SQL-запроса")
                    }
                }
            }
        }
    }
    post {
        cleanup {
                cleanWs()
        }
    }
}