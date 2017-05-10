pipeline {
    agent any // { docker 'openjdk:8-jdk' }

    stages {
        stage('everything') {
            steps {
                sh 'cp /var/my-gradle-files/gradle.properties .'
                sh './gradlew clean test'
            }
        }
    }

    post {
        always {
            junit '**/build/test-results/**/TEST-*.xml'
        }
    }
}
