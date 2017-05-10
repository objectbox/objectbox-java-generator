pipeline {
    agent any // { docker 'openjdk:8-jdk' }

    stages {
        stage('everything') {
            steps {
                sh 'cp /var/my-gradle-files/gradle.properties .'
                sh './gradlew clean check'
            }
        }
    }

    post {
        always {
            junit '**/build/test-results/**/TEST-*.xml'
        }

        always {
            mail to: emailToNotify,
            subject: "Build failed: ${currentBuild.fullDisplayName}",
            body: "${env.BUILD_URL}"
        }
    }
}
