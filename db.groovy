pipeline {
    agent any

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
                            PGPASSWORD=\"\$DB_PASS\" psql -h 88.151.117.221 -U \"\$DB_USER\" -d postgres_db -w <<EOF
                              SELECT * FROM public.cars';
                            EOF
                        """
                    }
                }
            }
        }
    }
}