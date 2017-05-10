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
        }

        changed {
            // For global vars see /jenkins/pipeline-syntax/globals
            slackSend message: "Changed ${currentBuild.currentResult}: ${currentBuild.fullDisplayName}\n${env.BUILD_URL}"
        }

        failure {
            // For global vars see /jenkins/pipeline-syntax/globals
            slackSend color: "warning",
                    message: "Failed: ${currentBuild.fullDisplayName}\n${env.BUILD_URL}"
        }
    }
}
