pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    environment {
        PATH = "/usr/local/bin:${env.PATH}"
        DOCKER_IMAGE_NAME = "singiattend-mongo"
        NEXUS_DOCKER_URL = "host.lima.internal:5001"  // Nexus Docker repo
        VERSION_FILE = "VERSION" // file storing Docker image version
    }

    parameters {
        booleanParam(name: 'PUSH_TO_NEXUS', defaultValue: false, description: 'Push Docker image to Nexus repository')
        choice(name: 'VERSION_ACTION', choices: ['none', 'bump-development', 'release'], description: 'Version action for Docker image')
        booleanParam(name: 'TAG_THIS_BUILD', defaultValue: false, description: 'Tag Docker image for this build')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Versioning') {
            when {
                expression { return params.VERSION_ACTION != 'none' }
            }
            steps {
                script {
                    // Read current version or default to 1.0.0-SNAPSHOT
                    def currentVersion = fileExists(VERSION_FILE) ? readFile(VERSION_FILE).trim() : "1.0.0-SNAPSHOT"
                    echo "Current Docker image version: ${currentVersion}"

                    if (params.VERSION_ACTION == 'bump-development') {
                        def parts = currentVersion.replace('-SNAPSHOT','').tokenize('.')
                        def major = parts[0].toInteger()
                        def minor = parts[1].toInteger() + 1
                        def nextDevVersion = "${major}.${minor}.0-SNAPSHOT"
                        writeFile file: VERSION_FILE, text: nextDevVersion
                        echo "Bumped Docker image to next development version: ${nextDevVersion}"
                    }

                    if (params.VERSION_ACTION == 'release') {
                        def releaseVersion = currentVersion.replace('-SNAPSHOT','-RC')
                        writeFile file: VERSION_FILE, text: releaseVersion
                        echo "Releasing Docker image version: ${releaseVersion}"
                    }

                    // Commit version file back to Git
                    def versionToCommit = readFile(VERSION_FILE).trim()
                    sh """
                        git config user.name "Марко Дојкић"
                        git config user.email "marko.dojkic@gmail.com"
                        git config user.signingkey F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537
                        git config commit.gpgsign true
                        git add ${VERSION_FILE}
                        if command -v gpg >/dev/null 2>&1; then
                          git commit -S -m 'Update Docker image version to ${versionToCommit}' || echo 'no commit needed'
                        else
                          git -c commit.gpgsign=false commit -m 'Update Docker image version to ${versionToCommit}' || echo 'no commit needed'
                        fi
                        git push origin HEAD:${env.BRANCH_NAME}
                    """
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    def versionTag = readFile(VERSION_FILE).trim()
                    echo "Building Docker image: ${DOCKER_IMAGE_NAME}:${versionTag}"
                    sh "docker build -t ${DOCKER_IMAGE_NAME}:${versionTag} -t ${DOCKER_IMAGE_NAME}:latest ."
                }
            }
        }

        stage('Push to Nexus') {
            when {
                expression { return params.PUSH_TO_NEXUS == true }
            }
            steps {
                script {
                    def versionTag = readFile(VERSION_FILE).trim()
                    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh """
                            echo \$NEXUS_PASS | docker login ${NEXUS_DOCKER_URL} -u \$NEXUS_USER --password-stdin
                            docker tag ${DOCKER_IMAGE_NAME}:${versionTag} ${NEXUS_DOCKER_URL}/${DOCKER_IMAGE_NAME}:${versionTag}
                            docker tag ${DOCKER_IMAGE_NAME}:latest ${NEXUS_DOCKER_URL}/${DOCKER_IMAGE_NAME}:latest
                            docker push ${NEXUS_DOCKER_URL}/${DOCKER_IMAGE_NAME}:${versionTag}
                            docker push ${NEXUS_DOCKER_URL}/${DOCKER_IMAGE_NAME}:latest
                            docker logout ${NEXUS_DOCKER_URL}
                        """
                    }
                }
            }
        }

        stage('Tag Build') {
            when {
                expression { return params.TAG_THIS_BUILD == true }
            }
            steps {
                script {
                    def versionTag = readFile(VERSION_FILE).trim()
                    echo "Creating Git tag: ${versionTag}"
                    sh """
                        git config user.name "Марко Дојкић"
                        git config user.email "marko.dojkic@gmail.com"
                        git config user.signingkey F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537
                        if command -v gpg >/dev/null 2>&1; then
                          git tag -s ${versionTag} -m "Tag version ${versionTag}"
                        else
                          git -c tag.gpgSign=false tag ${versionTag} -m "Tag version ${versionTag}"
                        fi
                        git push origin refs/tags/${versionTag}
                    """
                }
            }
        }

        stage('Cleanup') {
            steps {
                script {
                    def versionTag = readFile(VERSION_FILE).trim()
                    echo "Cleaning up local Docker images: ${DOCKER_IMAGE_NAME}:${versionTag}"
                    sh "docker rmi ${DOCKER_IMAGE_NAME}:${versionTag} || true"
                    sh "docker rmi ${DOCKER_IMAGE_NAME}:latest || true"
                }
            }
        }
    }

    post {
        success {
            echo "Docker build (and optional push) completed successfully!"
        }
        failure {
            echo "Docker build/push failed!"
        }
    }
}