pipeline {
    agent any // { docker 'openjdk:8-jdk' }
    stages {
        stage('build') {
            steps {
                sh 'cp ~/.gradle/gradle.properties .'
                sh './gradlew build'
            }
        }
    }
}
