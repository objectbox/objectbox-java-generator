def COLOR_MAP = ['SUCCESS': 'good', 'FAILURE': 'danger', 'UNSTABLE': 'danger', 'ABORTED': 'danger']

// To reclaim memory immediately after build, do not use Gradle daemon.
// To reduce memory usage, do not use Kotlin daemon, but compile in-process.
def gradleArgs = "-Dorg.gradle.daemon=false -Dkotlin.compiler.execution.strategy=\"in-process\" --stacktrace"
def isPublish = BRANCH_NAME == 'objectbox-publish'
String versionPostfix = BRANCH_NAME == 'objectbox-dev' ? 'dev'
                      : isPublish ? '' // build script detects empty string as not set
                      : BRANCH_NAME

pipeline {
    // It should be "agent none", but googlechatnotification requires a agent (bug?).
    // As a workaround we use label 'gchat' instead; don't use a agent used for stages here as it can deadlock.
    agent { label 'gchat' }

    environment {
        GITLAB_URL = credentials('gitlab_url')
        MVN_REPO_LOGIN = credentials('objectbox_internal_mvn_user')
        MVN_REPO_URL = credentials('objectbox_internal_mvn_repo_http')
        MVN_REPO_ARGS = "-PinternalObjectBoxRepo=$MVN_REPO_URL " +
                        "-PinternalObjectBoxRepoUser=$MVN_REPO_LOGIN_USR " +
                        "-PinternalObjectBoxRepoPassword=$MVN_REPO_LOGIN_PSW"
        MVN_REPO_UPLOAD_URL = credentials('objectbox_internal_mvn_repo')
        MVN_REPO_UPLOAD_ARGS = "-PpreferredRepo=$MVN_REPO_UPLOAD_URL " +
                        "-PpreferredUsername=$MVN_REPO_LOGIN_USR " +
                        "-PpreferredPassword=$MVN_REPO_LOGIN_PSW " +
                        "-PversionPostFix=$versionPostfix"
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
                        sh "./gradlew $gradleArgs $MVN_REPO_ARGS clean check"
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
                        bat "gradlew $gradleArgs $MVN_REPO_ARGS clean check"
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
                sh "./gradlew $gradleArgs $MVN_REPO_ARGS $MVN_REPO_UPLOAD_ARGS uploadArchives"
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
                // Note: add quotes around URL parameter to avoid line breaks due to semicolon in URL.
                sh "./gradlew $gradleArgs $MVN_REPO_ARGS " +
                   "\"-PpreferredRepo=${BINTRAY_URL}\" -PpreferredUsername=${BINTRAY_LOGIN_USR} -PpreferredPassword=${BINTRAY_LOGIN_PSW} " +
                   "uploadArchives"
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
