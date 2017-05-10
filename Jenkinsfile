pipeline {
    agent any // { docker 'openjdk:8-jdk' }

    stages {

        stage('config') {
            steps {
                sh 'cp /var/my-gradle-files/gradle.properties .'
                script {
                    def props = readProperties file: 'gradle.properties'
                    env.emailToNotify = props['emailToNotify']
                }
                echo "Email for notifications: ${env.emailToNotify}"
                //sh 'chmod +x gradlew'
                sh 'printenv'
            }
        }

        stage('build') {
            steps {
                sh './gradlew clean check'
            }
        }
    }

    post {
        always {
            junit '**/build/test-results/**/TEST-*.xml'

            echo "Sending notification to ${env.emailToNotify}"
            mail to: env.emailToNotify,
                subject: "Build failed: ${currentBuild.fullDisplayName}",
                body: "${env.BUILD_URL}"
            echo "Mail sent to ${env.emailToNotify}"
        }
    }
}
