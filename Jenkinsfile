pipeline {
    agent any // { docker 'openjdk:8-jdk' }
    stages {
        stage('build') {
            steps {
                sh 'cp /var/my-gradle-files/gradle.properties .'
                sh './gradlew classes'
            }
        }

        stage('test') {
            steps {
                try {
                    sh './gradlew test'
                } finally {
                    junit '**/build/test-results/**/TEST-*.xml'
                }
            }
        }
    }
}
