pipeline {
    agent any

    environment {
        POSTGRES_HOST     = credentials('postgres_host')
        POSTGRES_PORT     = '5432'
        POSTGRES_DBNAME   = 'postgres_db'
        POSTGRES_USERNAME = credentials('postgres_user')
        POSTGRES_PASSWORD = credentials('postgres_password')
    }

    parameters {
        string(name: 'TEMPERAURE', defaultValue: '', trim: true, description: 'Температура сейчас')
        string(name: 'WEAT', defaultValue: '', trim: true, description: 'Ощущения погоды')
        string(name: 'LOCATION', defaultValue: 'Очаково', trim: true, description: 'Место выполнения сборки')
        choice(name: 'День недели', choices: ['Понедельник', 'Вторник', 'Среда','Четверг', 'Пятница', 'Суббота', 'Воскресенье'], description: 'Выберите день недели')
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
                            PGPASSWORD=\"\$DB_PASS\" psql -h ${POSTGRES_HOST} -U \"\$DB_USER\" -d ${POSTGRES_DBNAME} -w <<EOF
                            insert into public.weather(weat, temperature, locaton, day_week) values ('Солнечно', 20, 'Очаково', 'Суббота')
                            EOF
                        """
                    }
                }
            }
        }
    }
}