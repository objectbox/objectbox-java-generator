def COLOR_MAP = ['SUCCESS': 'good', 'FAILURE': 'danger', 'UNSTABLE': 'danger', 'ABORTED': 'danger']

// To reclaim memory immediately after build, do not use Gradle daemon.
// To reduce memory usage, do not use Kotlin daemon, but compile in-process.
def gradleArgs = "-Dorg.gradle.daemon=false -Dkotlin.compiler.execution.strategy=\"in-process\" --stacktrace"
def isPublish = BRANCH_NAME == 'objectbox-publish'
String versionPostfix = BRANCH_NAME == 'objectbox-dev' ? 'dev'
                      : isPublish ? '' // build script detects empty string as not set
                      : BRANCH_NAME

// Note: using single quotes to avoid Groovy String interpolation leaking secrets.
def internalRepoArgs = '-PinternalObjectBoxRepo=$MVN_REPO_URL ' +
                '-PinternalObjectBoxRepoUser=$MVN_REPO_LOGIN_USR ' +
                '-PinternalObjectBoxRepoPassword=$MVN_REPO_LOGIN_PSW'
def internalRepoArgsBat = '-PinternalObjectBoxRepo=%MVN_REPO_URL% ' +
                '-PinternalObjectBoxRepoUser=%MVN_REPO_LOGIN_USR% ' +
                '-PinternalObjectBoxRepoPassword=%MVN_REPO_LOGIN_PSW%'
def uploadRepoArgs = '-PpreferredRepo=$MVN_REPO_UPLOAD_URL ' +
                '-PpreferredUsername=$MVN_REPO_LOGIN_USR ' +
                '-PpreferredPassword=$MVN_REPO_LOGIN_PSW '
// Note: add quotes around URL parameter to avoid line breaks due to semicolon in URL.
def uploadRepoArgsBintray = '\"-PpreferredRepo=$BINTRAY_URL\" ' +
                '-PpreferredUsername=$BINTRAY_LOGIN_USR ' +
                '-PpreferredPassword=$BINTRAY_LOGIN_PSW'

pipeline {
    // It should be "agent none", but googlechatnotification requires a agent (bug?).
    // As a workaround we use label 'gchat' instead; don't use a agent used for stages here as it can deadlock.
    agent { label 'gchat' }

    environment {
        MVN_REPO_LOGIN = credentials('objectbox_internal_mvn_user')
        MVN_REPO_URL = credentials('objectbox_internal_mvn_repo_http')
        MVN_REPO_UPLOAD_URL = credentials('objectbox_internal_mvn_repo')
        // Note: can't set key file here as it points to path, which must be agent-specific.
        ORG_GRADLE_PROJECT_signingKeyId = credentials('objectbox_signing_key_id')
        ORG_GRADLE_PROJECT_signingPassword = credentials('objectbox_signing_key_password')
    }

    options {
        gitLabConnection('objectbox-gitlab-connection')
    }

    stages {
        stage ('build') {
            parallel {
                stage('build-linux') {
                    agent { label 'linux' }
                    steps {
                        sh 'chmod +x gradlew'
                        sh "./gradlew -version"
                        sh "./gradlew $gradleArgs $internalRepoArgs clean check"
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
                        bat "gradlew -version"
                        bat "gradlew $gradleArgs $internalRepoArgsBat clean check"
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
            environment {
                // Note: for key use Jenkins secret file with PGP key as text in ASCII-armored format.
                ORG_GRADLE_PROJECT_signingKeyFile = credentials('objectbox_signing_key')
            }
            steps {
                sh "./gradlew $gradleArgs $internalRepoArgs $uploadRepoArgs -PversionPostFix=$versionPostfix uploadArchives"
            }
        }

        stage('upload-to-bintray') {
            when { expression { return isPublish } }
            agent { label 'linux' }

            environment {
                // Note: for key use Jenkins secret file with PGP key as text in ASCII-armored format.
                ORG_GRADLE_PROJECT_signingKeyFile = credentials('objectbox_signing_key')
                BINTRAY_URL = credentials('bintray_url')
                BINTRAY_LOGIN = credentials('bintray_login')
            }
            steps {
                googlechatnotification url: 'id:gchat_java',
                    message: "*Publishing* ${currentBuild.fullDisplayName} to Bintray...\n${env.BUILD_URL}"

                sh "./gradlew $gradleArgs $internalRepoArgs $uploadRepoArgsBintray uploadArchives"

                googlechatnotification url: 'id:gchat_java',
                    message: "Published ${currentBuild.fullDisplayName} successfully to Bintray - check https://bintray.com/objectbox/objectbox\n${env.BUILD_URL}"
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
