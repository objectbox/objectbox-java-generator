pipeline {
    agent any // { docker 'openjdk:8-jdk' }
    stages {
        stage('build') {
            steps {
                sh './gradlew'
            }
        }
    }
}
