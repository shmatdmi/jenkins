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
                    withCredentials([usernamePassword(
                        credentialsId: 'postgres-user-pass',
                        usernameVariable: 'DB_USER',
                        passwordVariable: 'DB_PASS'
                    )]) {
                        sh """
                            PGPASSWORD=\"\$DB_PASS\" psql -h ${POSTGRES_HOST} -p ${POSTGRES_PORT} -U \"\$DB_USER\" -d ${POSTGRES_DBNAME} -w <<EOF
                            select count(*) from public.stat_sport where type_sport = '${params.TYPE}';
                            EOF
                        """
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