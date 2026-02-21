pipeline {
      agent any

      environment {
          POSTGRES_HOST       = credentials('postgres_host')
          POSTGRES_PORT       = '5432'
          POSTGRES_DBNAME     = 'postgres_db'
          POSTGRES_USERNAME = credentials('postgres_user')
          POSTGRES_PASSWORD = credentials('postgres_password')
          MAIL="sberlinux@ya.ru"
      }

      options {
          timestamps()
          ansiColor('xterm')
      }

      parameters {
          choice(name: 'TYPE', choices: ['Отжимания', 'Подтягивания', 'Пресс','Приседания', 'Бег'], description: 'Выберите вид упражнений')
      }

      stages {
          stage('Database Query') {
              steps {
                  script {
                      def result = ""
                      withCredentials([usernamePassword(
                          credentialsId: 'postgres-user-pass',
                          usernameVariable: 'DB_USER',
                          passwordVariable: 'DB_PASS'
                      )]) {
                          result = sh(returnStdout: true, script: """
                              PGPASSWORD=\"\$DB_PASS\" psql -h ${POSTGRES_HOST} -p ${POSTGRES_PORT} -U \"\$DB_USER\" -d ${POSTGRES_DBNAME} -Atc "
                              select SUM(count_sport)::TEXT from public.stat_sport where type_sport = '${params.TYPE}' AND create_date >= NOW() - INTERVAL '7 days';
                              "
                          """).trim()
                      }
                    // Преобразование строки в целое число и деление пополам
                    def dividedResult = Integer.parseInt(result) / 7

                    // Сохранение результата деления в переменную окружения COUNT
                    env.DAY = "${dividedResult}"
                    env.COUNT = "${result}"

                    echo "Результат SQL-запроса: ${result}, поделённый на 7: ${dividedResult}"

                  }
              }
          }
      }
    post {
        cleanup {
                cleanWs()
        }
        success {
            mail to: "${env.MAIL}",
            subject: "Спорт за неделю",
            body: """${params.TYPE} всего: ${env.COUNT}, ${params.TYPE} в среднем за день: ${env.DAY}"""
        }
        failure {
            mail to: "${env.MAIL}",
            subject: "Failure project - Jenkins Pipeline: ${currentBuild.fullDisplayName}",
            body: "Failure project - Jenkins Pipeline: ${currentBuild.fullDisplayName}"
            echo 'Im failed'
        }
    }
}