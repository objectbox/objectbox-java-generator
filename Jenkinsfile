def COLOR_MAP = ['SUCCESS': 'good', 'FAILURE': 'danger', 'UNSTABLE': 'danger', 'ABORTED': 'danger']

def gradleArgs = '-Dorg.gradle.daemon=false --stacktrace'
def isPublish = BRANCH_NAME == 'objectbox-publish'
String versionPostfix = BRANCH_NAME == 'objectbox-dev' ? 'dev'
                      : isPublish ? '' // build script detects empty string as not set
                      : BRANCH_NAME

pipeline {
    agent none

    environment {
        GITLAB_URL = credentials('gitlab_url')
        MVN_REPO_URL = credentials('objectbox_internal_mvn_repo_http')
        MVN_REPO_URL_PUBLISH = credentials('objectbox_internal_mvn_repo')
        MVN_REPO_LOGIN = credentials('objectbox_internal_mvn_user')
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
                        sh "./gradlew $gradleArgs " +
                           "-PinternalObjectBoxRepo=${MVN_REPO_URL} -PinternalObjectBoxRepoUser=${MVN_REPO_LOGIN_USR} -PinternalObjectBoxRepoPassword=${MVN_REPO_LOGIN_PSW} " +
                           "clean check"
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
                        bat "gradlew $gradleArgs " +
                            "-PinternalObjectBoxRepo=${MVN_REPO_URL} -PinternalObjectBoxRepoUser=${MVN_REPO_LOGIN_USR} -PinternalObjectBoxRepoPassword=${MVN_REPO_LOGIN_PSW} " +
                            "clean check"
                    }
                    post {
                        always {
                            junit '**/build/test-results/**/TEST-*.xml'
                        }
                    }
                }

            }
        }

        stage('upload-to-internal') {
            agent { label 'linux' }
            steps {
                sh "./gradlew $gradleArgs -PinternalObjectBoxRepo=${MVN_REPO_URL} -PinternalObjectBoxRepoUser=${MVN_REPO_LOGIN_USR} -PinternalObjectBoxRepoPassword=${MVN_REPO_LOGIN_PSW} " +
                   "-PversionPostFix=${versionPostfix} " +
                   "-PpreferredRepo=${MVN_REPO_URL_PUBLISH} -PpreferredUsername=${MVN_REPO_LOGIN_USR} -PpreferredPassword=${MVN_REPO_LOGIN_PSW} " +
                   "uploadArchives"
            }
        }

        stage('upload-to-bintray') {
            when { expression { return isPublish } }
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
                sh "./gradlew -Dorg.gradle.daemon=false --stacktrace " +
                   "-PinternalObjectBoxRepo=${MVN_REPO_URL} -PinternalObjectBoxRepoUser=${MVN_REPO_LOGIN_USR} -PinternalObjectBoxRepoPassword=${MVN_REPO_LOGIN_PSW} " +
                   "-PpreferredRepo=${BINTRAY_URL} -PpreferredUsername=${BINTRAY_LOGIN_USR} -PpreferredPassword=${BINTRAY_LOGIN_PSW} " +
                   "uploadArchives"
                script {
                    slackSend color: "##41f4cd",
                            message: "Published ${currentBuild.fullDisplayName} successfully to Bintray - check https://bintray.com/objectbox/objectbox\n${env.BUILD_URL}"
                }
            }
        }
    }

    post {
        // For global vars see /jenkins/pipeline-syntax/globals

        always {
            googlechatnotification url: 'id:gchat_java', message: "${currentBuild.currentResult}: ${currentBuild.fullDisplayName}\n${env.BUILD_URL}",
                                   notifyFailure: 'true', notifyUnstable: 'true', notifyBackToNormal: 'true'
        }

        failure {
            updateGitlabCommitStatus name: 'build', state: 'failed'

            emailext (
                subject: "${currentBuild.currentResult}: ${currentBuild.fullDisplayName}",
                mimeType: 'text/html',
                recipientProviders: [[$class: 'DevelopersRecipientProvider']],
                body: """
                    <p>${currentBuild.currentResult}:
                        <a href='${env.BUILD_URL}'>${currentBuild.fullDisplayName}</a>
                        (<a href='${env.BUILD_URL}/console'>console</a>)
                    </p>
                    <p>Git: ${GIT_COMMIT} (${GIT_BRANCH})
                    <p>Build time: ${currentBuild.durationString}
                """
            )
        }

        success {
            updateGitlabCommitStatus name: 'build', state: 'success'
        }
    }
}
