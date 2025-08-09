pipeline {
    agent any

    environment {
        POSTGRES_HOST     = credentials('postgres_host')
        POSTGRES_PORT     = '5432'
        POSTGRES_DBNAME   = 'postgres_db'
        POSTGRES_USERNAME = credentials('postgres_user')
        POSTGRES_PASSWORD = credentials('postgres_password')
    }

    stages {
        stage('Update Database') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'postgres-user-pass',
                                                     usernameVariable: 'DB_USER',
                                                     passwordVariable: 'DB_PASS')]) {
                        try {
                            sh '''
                                export PGPASSWORD="${DB_PASS}"
                                psql -h ${POSTGRES_HOST} \
                                     -p ${POSTGRES_PORT} \
                                     -U ${DB_USER} \
                                     -d ${POSTGRES_DBNAME} \
                                     -c "SELECT * FROM public.cars;"
                            '''
                        } finally {
                            unset PGPASSWORD
                        }
                    }
                }
            }
        }
    }
}