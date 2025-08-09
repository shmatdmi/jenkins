pipeline {
    agent any

    environment {
        POSTGRES_HOST     = credentials('postgres_host')
        POSTGRES_PORT     = '5432'
        POSTGRES_DBNAME   = 'mydatabase'
        POSTGRES_USERNAME = credentials('postgres_user')
        POSTGRES_PASSWORD = credentials('postgres_password')
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
                            PGPASSWORD=\"\$DB_PASS\" psql -h ${POSTGRES_HOST} -U \"\$DB_USER\" -d postgres_db -w <<EOF
                              SELECT * FROM public.cars;
                              update cars set color = 'Blue', price = 11000 where "year" = 2002;
                              SELECT * FROM public.cars;
                            EOF

                        """
                    }
                }
            }
        }
    }
}