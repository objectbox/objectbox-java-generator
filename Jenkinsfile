pipeline {
    agent any // { docker 'openjdk:8-jdk' }
    stages {
        stage('build') {
            steps {
                sh 'cp /var/my-gradle-files/gradle.properties .'
                sh './gradlew build'
            }
        }
    }
}
