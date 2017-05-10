pipeline {
    agent any // { docker 'openjdk:8-jdk' }

    stages {
        stage('config') {
            steps {
                sh 'cp /var/my-private-files/private.properties ./gradle.properties'
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
            slackSend channel: '#jenkins',
                    message: "Pipeline ${currentBuild.fullDisplayName} completed: ${env.BUILD_URL}"
        }
    }
}
