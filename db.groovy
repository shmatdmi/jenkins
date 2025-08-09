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
                        sh """
                            export PGPASSWORD=\"\$DB_PASS\"
                            psql -h 88.151.117.221 -U \"\$DB_USER\" -d postgres_db <<EOF
                              SELECT * FROM public.cars;
                            EOF
                        """
                        } finally {
                            unset PGPASSWORD
                        }
                    }
                }
            }
        }
    }
}