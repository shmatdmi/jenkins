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
        choice(name: 'TYPE', choices: ['Подтягивания', 'Отжимания', 'Пресс','Приседания', 'Бег'], description: 'Выберите вид упражений')
        string(name: 'COUNT', defaultValue: '', trim: true, description: 'Количество упражнений')
        string(name: 'LOCATION', defaultValue: 'Очаково', trim: true, description: 'Место выполнения упраженения')
        string(name: 'TIME', defaultValue: '', trim: true, description: 'Время выполнения')
    }

    stages {
        stage('Database Update') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'postgres-user-pass',
                        usernameVariable: 'DB_USER',
                        passwordVariable: 'DB_PASS'
                    )]) {
                        sh """
                            PGPASSWORD=\"\$DB_PASS\" psql -h ${POSTGRES_HOST} -p ${POSTGRES_PORT} -U \"\$DB_USER\" -d ${POSTGRES_DBNAME} -w <<EOF
                            insert into public.stat_sport(type_sport, count_sport, location_sport, time_sport) values ('${params.TYPE}', ${params.COUNT}, '${params.LOCATION}', '${params.TIME}');
                            EOF
                        """
                    }
                }
            }
        }
    }
}