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
            // For global vars see /jenkins/pipeline-syntax/globals
            script {
                env.MY_COLOR=currentBuild.currentBuild.result == 'SUCCESS'? 'good': 'warning'
            }
            slackSend color: "${env.MY_COLOR}",
                    message: "${currentBuild.fullDisplayName} completed: ${env.BUILD_URL}, by @${env.CHANGE_AUTHOR}"
        }
    }
}
