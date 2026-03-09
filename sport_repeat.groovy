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
        // Параметры упражнений
        choice(name: 'TYPE', choices: ['Отжимания', 'Подтягивания', 'Пресс','Приседания', 'Бег'], description: 'Выберите вид упражнения')
        string(name: 'COUNT', defaultValue: '39', trim: true, description: 'Количество повторений')
        string(name: 'LOCATION', defaultValue: 'Очаково', trim: true, description: 'Место выполнения упражнения')
        string(name: 'TIME', defaultValue: '1', trim: true, description: 'Время выполнения')
        string(name: 'REPEAT_TIMES', defaultValue: '2', trim: true, description: 'Количество повторений сборки')
    }

    stages {
        stage('Database Update') {
            steps {
                script {
                    def repeatTimes = params.REPEAT_TIMES.toInteger() ?: 1
                    
                    for (def i = 1; i <= repeatTimes; i++) {
                        echo "Выполнение шага №${i}"
                        
                        withCredentials([usernamePassword(
                            credentialsId: 'postgres-user-pass',
                            usernameVariable: 'DB_USER',
                            passwordVariable: 'DB_PASS'
                        )]) {
                            sh """
                                PGPASSWORD="\$DB_PASS" psql -h ${POSTGRES_HOST} -p ${POSTGRES_PORT} -U "\$DB_USER" -d ${POSTGRES_DBNAME} -w <<EOF
                                insert into public.stat_sport(type_sport, count_sport, location_sport, time_sport) values ('${params.TYPE}', ${params.COUNT}, '${params.LOCATION}', '${params.TIME}');
                                EOF
                            """
                        }
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