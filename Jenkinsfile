pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    environment {
        // Tools (ensure these names exist in Manage Jenkins -> Global Tool Configuration)
        JAVA_HOME = tool name: 'JDK 17', type: 'jdk'
        MAVEN_HOME = tool name: 'Maven 3.9', type: 'maven'

        // ensure typical Homebrew paths are available (gpg, mvn, docker)
        PATH = "${JAVA_HOME}/bin:${MAVEN_HOME}/bin:/usr/local/bin:/opt/homebrew/bin:${env.PATH}"

        // Nexus / Sonar credentials are injected via withCredentials in stages — keep only ids here
        NEXUS_RELEASE_URL = "http://localhost:8081/repository/maven-releases/"
        NEXUS_SNAPSHOT_URL = "http://localhost:8081/repository/maven-snapshots/"
        SONAR_URL = "http://localhost:9000"

        // GPG home path for informational use (adjust if needed)
        GPG_HOME = "/Users/markodojkic/.gnupg"
    }

    parameters {
        booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run SonarQube analysis')
        choice(name: 'VERSION_ACTION', choices: ['none', 'bump-development', 'release'], description: 'Versioning action')
        booleanParam(name: 'TAG_THIS_BUILD', defaultValue: false, description: 'Create Git tag')
    }

    stages {
        stage('Checkout & Prepare Branch') {
            steps {
                script {
                    // Explicit checkout of the branch (avoids detached HEAD)
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "refs/heads/${env.BRANCH_NAME}"]],
                        doGenerateSubmoduleConfigurations: false,
                        userRemoteConfigs: [[
                            url: 'https://github.com/MarkoDojkic/SingiAttend_Multibranch_Jenkins.git',
                            credentialsId: 'github-creds'
                        ]]
                    ])

                    // make sure local branch exists and tracks remote
                    sh "git checkout -B ${env.BRANCH_NAME} origin/${env.BRANCH_NAME} || true"

                    // detect changes (safe: if repo has only one commit Git diff HEAD~1 may fail)
                    def changes = sh(script: "git rev-parse --verify HEAD~1 >/dev/null 2>&1 && git diff --name-only HEAD~1 HEAD || true", returnStdout: true).trim()
                    if (!changes) {
                        echo "No changes detected. Skipping build."
                        currentBuild.result = 'NOT_BUILT'
                        error("Stopping pipeline: no changes")
                    }
                }
            }
        }

        stage('POM Info') {
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    echo "Project Name: ${pom.name}"
                    echo "ArtifactId: ${pom.artifactId}"
                    echo "Version: ${pom.version}"
                }
            }
        }

        stage('Versioning') {
            when { expression { return params.VERSION_ACTION != 'none' } }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    def currentVersion = pom.version
                    echo "Current version: ${currentVersion}"

                    if (params.VERSION_ACTION == 'bump-development') {
                        def versionParts = currentVersion.replace('-SNAPSHOT','').tokenize('.')
                        def major = versionParts[0].toInteger()
                        def minor = versionParts[1].toInteger() + 1
                        def nextDevVersion = "${major}.${minor}.0-SNAPSHOT"

                        sh "mvn versions:set -DnewVersion=${nextDevVersion}"
                        sh "mvn versions:commit"
                        echo "Next development version set to: ${nextDevVersion}"

                        // Ensure we're on branch, then attempt signed commit; if gpg not available, fallback to unsigned
                        sh """
                            git checkout -B ${env.BRANCH_NAME} origin/${env.BRANCH_NAME}
                            git config user.name "Марко Дојкић"
                            git config user.email "marko.dojkic@gmail.com"
                            git config user.signingkey F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537
                            git add pom.xml
                            if command -v gpg >/dev/null 2>&1; then
                              git commit -S -m 'Bump version to ${nextDevVersion}' || echo 'no commit (no changes)'
                            else
                              git -c commit.gpgsign=false commit -m 'Bump version to ${nextDevVersion}' || echo 'no commit (no changes)'
                            fi
                            git push origin HEAD:${env.BRANCH_NAME}
                        """
                    }

                    if (params.VERSION_ACTION == 'release') {
                        def releaseVersion = currentVersion.replace('-SNAPSHOT','')
                        sh "mvn versions:set -DnewVersion=${releaseVersion}"
                        sh "mvn versions:commit"
                        echo "Releasing version: ${releaseVersion}"

                        sh """
                            git checkout -B ${env.BRANCH_NAME} origin/${env.BRANCH_NAME}
                            git config user.name "Марко Дојкић"
                            git config user.email "marko.dojkic@gmail.com"
                            git config user.signingkey F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537
                            git add pom.xml
                            if command -v gpg >/dev/null 2>&1; then
                              git commit -S -m 'Release version ${releaseVersion}' || echo 'no commit (no changes)'
                            else
                              git -c commit.gpgsign=false commit -m 'Release version ${releaseVersion}' || echo 'no commit (no changes)'
                            fi
                            git push origin HEAD:${env.BRANCH_NAME}
                        """
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean install -DskipTests -B'
            }
        }

        stage('Sonar Analysis') {
            when { expression { return params.RUN_SONAR == true } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'sonar-creds', usernameVariable: 'SONAR_USER', passwordVariable: 'SONAR_PASS')]) {
                    // SONAR_PASS is used inside the shell as an env var (no Groovy interpolation of secret)
                    sh """
                        mvn sonar:sonar \
                          -Dsonar.projectKey=SingiAttend-Server-Jenkins \
                          -Dsonar.host.url=${SONAR_URL} \
                          -Dsonar.login=\$SONAR_PASS
                    """
                }
            }
        }

        stage('Deploy to Nexus') {
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    def deployUrl = pom.version.endsWith("SNAPSHOT") ? NEXUS_SNAPSHOT_URL : NEXUS_RELEASE_URL

                    sh """
                        mvn deploy \
                            -Dnexus.url=${deployUrl}
                    """
                }
            }
        }

        stage('Tag Build') {
            when { expression { return params.TAG_THIS_BUILD == true } }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    def version = pom.version.replace('-SNAPSHOT','')
                    echo "Creating Git tag: ${version}"

                    sh """
                        git checkout -B ${env.BRANCH_NAME} origin/${env.BRANCH_NAME}
                        git config user.name "Марко Дојкић"
                        git config user.email "marko.dojkic@gmail.com"
                        git config user.signingkey F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537
                        if command -v gpg >/dev/null 2>&1; then
                          git tag -s ${version} -m "Tag version ${version}"
                        else
                          git -c tag.gpgSign=false tag ${version} -m "Tag version ${version}"
                        fi
                        git push origin refs/tags/${version}
                    """
                }
            }
        }

        stage('Cleanup') {
            steps {
                echo "Cleaning build files..."
                sh 'mvn clean -B'
                sh 'rm -rf target/*'
            }
        }
    }

    post {
        success {
            echo "Build and deployment completed successfully!"
        }
        failure {
            echo "Build failed!"
        }
    }
}