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
                script {
                    env.MY_COLOR = 'warning'
                }
                sh './gradlew clean check'
                script {
                    env.MY_COLOR = 'good'
                }
            }
        }
    }

    post {
        always {
            junit '**/build/test-results/**/TEST-*.xml'

            // For global vars see /jenkins/pipeline-syntax/globals
            slackSend color: "${env.MY_COLOR}",
                    message: "${currentBuild.fullDisplayName}: ${currentBuild.result}\n${env.BUILD_URL}"
        }
    }
}
