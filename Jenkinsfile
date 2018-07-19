def COLOR_MAP = ['SUCCESS': 'good', 'FAILURE': 'danger', 'UNSTABLE': 'danger', 'ABORTED': 'danger']

pipeline {
    agent any // { docker 'openjdk:8-jdk' }

    stages {
        stage('build-linux') {
            agent { label 'linux' }
            steps {
                // sh 'cp /var/my-private-files/private.properties ./gradle.properties'
                sh 'chmod +x gradlew'
                sh './gradlew clean check install'
            }
        }
        stage('build-windows') {
            agent { label 'windows' }
            steps {
                bat 'gradlew clean check install'
            }
        }
    }

    post {
        always {
            junit '**/build/test-results/**/TEST-*.xml'
        }

        changed {
            // For global vars see /jenkins/pipeline-syntax/globals
            slackSend color: COLOR_MAP[currentBuild.currentResult],
                    message: "Changed to ${currentBuild.currentResult}: ${currentBuild.fullDisplayName}\n${env.BUILD_URL}"
        }

        failure {
            // For global vars see /jenkins/pipeline-syntax/globals
            slackSend color: "danger",
                    message: "Failed: ${currentBuild.fullDisplayName}\n${env.BUILD_URL}"
        }
    }
}
