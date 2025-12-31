// === Helper: Git + GPG setup (Jenkins-safe) ===
def setupGitConfig() {
    sh '''
        mkdir -p "$GPG_HOME"
        chmod 700 "$GPG_HOME"

        # Git config with Unicode name and GPG signing
        git config --global user.name "Марко Дојкић"
        git config --global user.email "marko.dojkic@gmail.com"
        git config --global user.signingkey ''' + env.GPG_KEY_ID + '''
        git config --global commit.gpgsign true
        git config --global tag.gpgsign true

        # Configure GPG program and format
        git config --global gpg.program "$(which gpg)"
        git config --global gpg.format openpgp

        # Create gpg.conf with non-interactive settings
        echo "use-agent" > "$GPG_HOME/gpg.conf"
        echo "pinentry-mode loopback" >> "$GPG_HOME/gpg.conf"
        chmod 600 "$GPG_HOME/gpg.conf"

        # Create gpg-agent.conf for Jenkins non-interactive use
        echo 'export GPG_TTY=$(tty)' > "$GPG_HOME/gpg-agent.conf"
        chmod 600 "$GPG_HOME/gpg-agent.conf"
    '''
}

pipeline {
    agent any

    options {
        ansiColor('xterm')
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    environment {
        NEXUS_RELEASE_URL = "https://nexus.markodojkic.local/repository/maven-releases/"
        NEXUS_SNAPSHOT_URL = "https://nexus.markodojkic.local/repository/maven-snapshots/"
        SONAR_URL = "https://sonar.markodojkic.local"

        GPG_KEY_ID = "F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537"
        GPG_HOME = "${WORKSPACE}/.gnupg"
        PATH = "/usr/local/MacGPG2/bin:/usr/local/bin:/opt/homebrew/bin:/usr/bin:${env.PATH}"

        DOCKER_IMAGE_NAME = "singiattend-server"
        NEXUS_DOCKER_URL = "markodojkic.local:5001"
        DOCKER_BUILDKIT = '1'
    }

    parameters {
        booleanParam(name: 'SKIP_BUILD', defaultValue: false, description: 'Skip the entire build process')
        booleanParam(name: 'SKIP_DEPLOY', defaultValue: false, description: 'Skip deployment to Nexus')
        booleanParam(name: 'SKIP_DOCKER', defaultValue: false, description: 'Skip Docker build')
        booleanParam(name: 'SKIP_DOCKER_DEPLOY', defaultValue: false, description: 'Skip Docker deployment to Nexus')
        booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run SonarQube analysis')
        choice(name: 'VERSION_ACTION', choices: ['none', 'bump-development', 'release'], description: 'Versioning action')
        booleanParam(name: 'TAG_THIS_BUILD', defaultValue: false, description: 'Create Git tag')
    }

    stages {
        stage('Check Skip Options') {
            when { expression { params.SKIP_BUILD } }
            steps {
                echo "Build skipped as requested."
                script { currentBuild.result = 'SUCCESS' }
            }
        }

        stage('Checkout & Setup') {
            steps {
                script {
                    checkout scm
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

        stage('Version Management') {
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

                        withMaven(maven: 'Maven 4.0', jdk: 'JDK 25') {
                            sh "mvn versions:set -DnewVersion=${nextDevVersion}"
                            sh "mvn versions:commit"
                        }

                        echo "Next development version set to: ${nextDevVersion}"

                        withCredentials([string(credentialsId: 'gpg-passphrase', variable: 'GPG_PASS')]) {
                            sh """
                                git add pom.xml
                                printf "%s" "${GPG_PASS}" | git commit -S -m "chore: bump version to ${nextDevVersion}" || echo "No changes"
                                git push origin HEAD:${env.BRANCH_NAME} || true
                            """
                        }
                    }

                    if (params.VERSION_ACTION == 'release') {
                        def releaseVersion = currentVersion.replace('-SNAPSHOT','')
                        withMaven(maven: 'Maven 4.0', jdk: 'JDK 25') {
                            sh "mvn versions:set -DnewVersion=${releaseVersion}"
                            sh "mvn versions:commit"
                        }

                        echo "Releasing version: ${releaseVersion}"

                        withCredentials([string(credentialsId: 'gpg-passphrase', variable: 'GPG_PASS')]) {
                            sh """
                                git add pom.xml
                                printf "%s" "${GPG_PASS}" | git commit -S -m "Release version ${releaseVersion}" || echo "No changes"
                                git push origin HEAD:${env.BRANCH_NAME} || true
                            """
                        }
                    }
                }
            }
        }

        stage('Build') {
            when { expression { !params.SKIP_BUILD } }
            steps {
                withMaven(maven: 'Maven 4.0', jdk: 'JDK 25') {
                    sh 'mvn clean install -DskipTests -B'
                }
            }
        }

        stage('Sonar Analysis') {
            when {
                allOf {
                    expression { !params.SKIP_BUILD }
                    expression { params.RUN_SONAR }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'sonar-creds', usernameVariable: 'SONAR_USER', passwordVariable: 'SONAR_PASS')]) {
                    withMaven(maven: 'Maven 4.0', jdk: 'JDK 25') {
                        sh """
                            mvn sonar:sonar \
                              -Dsonar.projectKey=SingiAttend-Server-Jenkins \
                              -Dsonar.host.url=${SONAR_URL} \
                              -Dsonar.login=\$SONAR_PASS
                        """
                    }
                }
            }
        }

        stage('Deploy to Nexus') {
            when {
                allOf {
                    expression { !params.SKIP_BUILD }
                    expression { !params.SKIP_DEPLOY }
                }
            }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    def deployUrl = pom.version.endsWith("SNAPSHOT") ? NEXUS_SNAPSHOT_URL : NEXUS_RELEASE_URL

                    withMaven(maven: 'Maven 4.0', jdk: 'JDK 25') {
                        sh """
                            mvn deploy \
                                -Dnexus.url=${deployUrl}
                        """
                    }
                }
            }
        }

        stage('Build Docker Image') {
            when { expression { !params.SKIP_DOCKER } }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    echo "Building Docker image ${env.DOCKER_IMAGE_NAME}:${pom.version}"

                    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh """
                            echo \$NEXUS_PASS | docker login ${NEXUS_DOCKER_URL} -u \$NEXUS_USER --password-stdin
                            docker pull ${NEXUS_DOCKER_URL}/${DOCKER_IMAGE_NAME}:latest || true

                            DOCKER_BUILDKIT=1 docker build \
                                --cache-from ${NEXUS_DOCKER_URL}/${DOCKER_IMAGE_NAME}:latest \
                                --build-arg VERSION=${pom.version} \
                                -t ${DOCKER_IMAGE_NAME}:${pom.version} \
                                -t ${DOCKER_IMAGE_NAME}:latest .
                        """
                    }
                }
            }
        }

        stage('Push Docker Image to Nexus') {
            when { expression { !params.SKIP_DOCKER_DEPLOY } }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh """
                            echo \$NEXUS_PASS | docker login ${NEXUS_DOCKER_URL} -u \$NEXUS_USER --password-stdin
                            docker tag ${DOCKER_IMAGE_NAME}:${pom.version} ${NEXUS_DOCKER_URL}/${DOCKER_IMAGE_NAME}:${pom.version}
                            docker tag ${DOCKER_IMAGE_NAME}:latest ${NEXUS_DOCKER_URL}/${DOCKER_IMAGE_NAME}:latest
                            docker push ${NEXUS_DOCKER_URL}/${DOCKER_IMAGE_NAME}:${pom.version}
                            docker push ${NEXUS_DOCKER_URL}/${DOCKER_IMAGE_NAME}:latest
                            docker logout ${NEXUS_DOCKER_URL}
                        """
                    }
                }
            }
        }

        stage('Tag Build') {
            when { expression { params.TAG_THIS_BUILD } }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    def version = pom.version

                    withCredentials([string(credentialsId: 'gpg-passphrase', variable: 'GPG_PASS')]) {
                        sh """
                             git fetch --tags --force
                             git tag -d singiattend-server-${version} 2>/dev/null || true
                             git push origin :refs/tags/singiattend-server-${version} 2>/dev/null || true

                             printf "%s" "$GPG_PASS" | git -c gpg.passphrase-fd=0 tag -s -u "$GPG_KEY_ID" \
                                 -m "Release singiattend/server:${version} (branch: ${BRANCH_NAME})" \
                                 singiattend-server-${version}

                             git verify-tag -v singiattend-server-${version}
                             git push origin singiattend-server-${version}
                        """
                    }
                }
            }
        }

        stage('Cleanup') {
            steps {
                echo "Cleaning build files..."
                withMaven(maven: 'Maven 4.0', jdk: 'JDK 25') {
                    sh 'mvn clean -B'
                }
                sh 'rm -rf target/*'
            }
        }
    }

    post {
        success {
            echo "✅ Build and deployment completed successfully!"
        }
        failure {
            echo "❌ Build failed!"
        }
        cleanup {
            // Ensure workspace is always cleaned up, even if build is aborted
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}