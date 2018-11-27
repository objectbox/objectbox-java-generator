def COLOR_MAP = ['SUCCESS': 'good', 'FAILURE': 'danger', 'UNSTABLE': 'danger', 'ABORTED': 'danger']

def gradleArgs = '-Dorg.gradle.daemon=false --stacktrace clean check install'

pipeline {
    agent none

    environment {
        GITLAB_URL = credentials('gitlab_url')
    }

    options {
        gitLabConnection("${env.GITLAB_URL}")
    }

    stages {
        stage ('build') {
            parallel {
                stage('build-linux') {
                    agent { label 'linux' }
                    steps {
                        sh 'chmod +x gradlew'
                        sh "./gradlew $gradleArgs"
                    }
                    post {
                        always {
                            junit '**/build/test-results/**/TEST-*.xml'
                        }
                    }
                }

                stage('build-windows') {
                    agent { label 'windows' }
                    steps {
                        bat "gradlew $gradleArgs"
                    }
                    post {
                        always {
                            junit '**/build/test-results/**/TEST-*.xml'
                        }
                    }
                }

            }
        }

        stage('upload-to-bintray') {
            when { expression { return BRANCH_NAME == 'objectbox-publish' } }
            agent { label 'linux' }

            environment {
                BINTRAY_URL = credentials('bintray_url')
                BINTRAY_LOGIN = credentials('bintray_login')
            }
            steps {
                script {
                    slackSend color: "#42ebf4",
                            message: "Publishing ${currentBuild.fullDisplayName} to Bintray...\n${env.BUILD_URL}"
                }
                sh './gradlew -Dorg.gradle.daemon=false --stacktrace -PpreferredRepo=${BINTRAY_URL} -PpreferredUsername=${BINTRAY_LOGIN_USR} -PpreferredPassword=${BINTRAY_LOGIN_PSW} uploadArchives'
                script {
                    slackSend color: "##41f4cd",
                            message: "Published ${currentBuild.fullDisplayName} successfully to Bintray - check https://bintray.com/objectbox/objectbox\n${env.BUILD_URL}"
                }
            }
        }
    }

    post {
        changed {
            // For global vars see /jenkins/pipeline-syntax/globals
            slackSend color: COLOR_MAP[currentBuild.currentResult],
                    message: "Changed to ${currentBuild.currentResult}: ${currentBuild.fullDisplayName}\n${env.BUILD_URL}"
        }

        failure {
            // For global vars see /jenkins/pipeline-syntax/globals
            slackSend color: "danger",
                    message: "Failed: ${currentBuild.fullDisplayName}\n${env.BUILD_URL}"
            updateGitlabCommitStatus name: 'build', state: 'failed'
        }

        success {
            updateGitlabCommitStatus name: 'build', state: 'success'
        }
    }
}
