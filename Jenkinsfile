def COLOR_MAP = ['SUCCESS': 'good', 'FAILURE': 'danger', 'UNSTABLE': 'danger', 'ABORTED': 'danger']

// To reclaim memory immediately after build, do not use Gradle daemon.
// To reduce memory usage, do not use Kotlin daemon, but compile in-process.
def gradleArgs = "-Dorg.gradle.daemon=false -Dkotlin.compiler.execution.strategy=\"in-process\" --stacktrace"
def isPublish = BRANCH_NAME == 'objectbox-publish'
String versionPostfix = BRANCH_NAME == 'objectbox-dev' ? 'dev'
                      : isPublish ? '' // build script detects empty string as not set
                      : BRANCH_NAME

// Note: using single quotes to avoid Groovy String interpolation leaking secrets.
def gitlabRepoArgs = '-PgitlabUrl=$GITLAB_URL -PgitlabPrivateToken=$GITLAB_TOKEN'
def gitlabRepoArgsBat = '-PgitlabUrl=%GITLAB_URL% -PgitlabPrivateToken=%GITLAB_TOKEN%'
def uploadRepoArgsCentral = '-PsonatypeUsername=$OSSRH_LOGIN_USR -PsonatypePassword=$OSSRH_LOGIN_PSW'

pipeline {
    // It should be "agent none", but googlechatnotification requires a agent (bug?).
    // As a workaround we use label 'gchat' instead; don't use a agent used for stages here as it can deadlock.
    agent { label 'gchat' }

    environment {
        GITLAB_URL = credentials('gitlab_url')
        GITLAB_TOKEN = credentials('GITLAB_TOKEN_ALL')
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
                        sh "./gradlew $gradleArgs $gitlabRepoArgs clean check"
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
                        bat "gradlew $gradleArgs $gitlabRepoArgsBat clean check"
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
                sh "./gradlew $gradleArgs $gitlabRepoArgs -PversionPostFix=$versionPostfix publishMavenJavaPublicationToGitLabRepository"
            }
        }

        stage('upload-to-central') {
            when { expression { return isPublish } }
            agent { label 'linux' }

            environment {
                // Note: for key use Jenkins secret file with PGP key as text in ASCII-armored format.
                ORG_GRADLE_PROJECT_signingKeyFile = credentials('objectbox_signing_key')
                OSSRH_LOGIN = credentials('ossrh-login')
            }
            steps {
                googlechatnotification url: 'id:gchat_java',
                    message: "*Publishing* ${currentBuild.fullDisplayName} to Central...\n${env.BUILD_URL}"

                // Step 1: upload files to staging repository.
                sh "./gradlew $gradleArgs $gitlabRepoArgs $uploadRepoArgsCentral publishMavenJavaPublicationToSonatypeRepository"

                // Step 2: close and release staging repository.
                sh "./gradlew $gradleArgs $gitlabRepoArgs $uploadRepoArgsCentral closeAndReleaseRepository"

                googlechatnotification url: 'id:gchat_java',
                    message: "Published ${currentBuild.fullDisplayName} successfully to Central - check https://repo1.maven.org/maven2/io/objectbox/ in a few minutes.\n${env.BUILD_URL}"
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
