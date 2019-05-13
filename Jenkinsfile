def COLOR_MAP = ['SUCCESS': 'good', 'FAILURE': 'danger', 'UNSTABLE': 'danger', 'ABORTED': 'danger']

def gradleArgs = '-Dorg.gradle.daemon=false --stacktrace'

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
                        sh "./gradlew $gradleArgs clean check"
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
                        bat "gradlew $gradleArgs clean check"
                    }
                    post {
                        always {
                            junit '**/build/test-results/**/TEST-*.xml'
                        }
                    }
                }

            }
        }

        stage('upload-to-repo') {
            when { expression { return BRANCH_NAME != 'objectbox-publish' } }
            agent { label 'linux' }
            environment {
                MVN_REPO_URL = credentials('objectbox_internal_mvn_repo')
                MVN_REPO_LOGIN = credentials('objectbox_internal_mvn_user')
            }
            steps {
                sh "./gradlew $gradleArgs -PpreferredRepo=${MVN_REPO_URL} -PpreferredUsername=${MVN_REPO_LOGIN_USR} -PpreferredPassword=${MVN_REPO_LOGIN_PSW} uploadArchives"
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
        // For global vars see /jenkins/pipeline-syntax/globals
        failure {
            updateGitlabCommitStatus name: 'build', state: 'failed'
        }

        success {
            updateGitlabCommitStatus name: 'build', state: 'success'
        }
    }
}
