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
        JOB_TMP_FOLDER = "/tmp/jenkins/${JOB_NAME}/${BUILD_NUMBER}"
        GPG_KEY_ID = "F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537"
        GPG_SCRIPT = "${JOB_TMP_FOLDER}/gpg-wrapper.sh"
        
        PATH = "/usr/bin:/usr/local/bin/:${env.PATH}"
        DOCKER_IMAGE_NAME = "singiattend-appstack"
        NEXUS_DOCKER_URL = "nexus.markodojkic.local"
        SONAR_URL = "https://sonar.markodojkic.qzz.io"
        VERSION_FILE = "VERSION"
        DOCKER_BUILDKIT = '1'
    }

    parameters {
        booleanParam(name: 'SKIP_DOCKER', defaultValue: false, description: 'Skip Docker build')
        booleanParam(name: 'SKIP_DOCKER_DEPLOY', defaultValue: false, description: 'Skip Docker deployment to Nexus')
        booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run SonarQube analysis')
        credentials(
            name: 'SONAR_CREDENTIALS_ID',
            defaultValue: '',
            description: 'Select Sonar token credential',
            credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl'
        )
        choice(name: 'VERSION_ACTION', choices: ['none', 'bump-patch', 'bump-minor', 'bump-major', 'release'], description: 'Versioning action')
        booleanParam(name: 'TAG_THIS_BUILD', defaultValue: false, description: 'Create Git tag')
    }

    stages {
        stage('Setup GPG') {
            steps {
                script {
                    withCredentials([
                        file(credentialsId: 'gpg-secret-key', variable: 'GPG_KEY_FILE'),
                        string(credentialsId: 'gpg-passphrase', variable: 'GPG_PASSPHRASE')
                    ]) {
                        sh """
                            set +x
                            export GNUPGHOME="${JOB_TMP_FOLDER}/.gnupg"

                            mkdir -p "\$GNUPGHOME"
                            chmod 700 "\$GNUPGHOME"

                            tr -d '\\r' < "${GPG_KEY_FILE}" > "${GPG_KEY_FILE}.fixed"
                            mv "${GPG_KEY_FILE}.fixed" "${GPG_KEY_FILE}"

                            gpgconf --kill gpg-agent || true

                            gpg --batch --import "${GPG_KEY_FILE}"

                            printf '%s\n' \
                            '#!/bin/bash' \
                            'export GNUPGHOME="'"${JOB_TMP_FOLDER}"'/.gnupg"' \
                            'exec gpg --batch --yes --pinentry-mode loopback --passphrase "'"${GPG_PASSPHRASE}"'" "\$@"' \
                            > "${GPG_SCRIPT}"

                            chmod 755 "${GPG_SCRIPT}"
                            set -x
                        """
                    }
                }
            }
        }
            
        stage('Checkout & Setup Git') {
            steps {
                checkout([
                        $class: 'GitSCM',
                        branches: [[name: "refs/heads/${env.BRANCH_NAME}"]],
                        userRemoteConfigs: [[
                            url: 'https://github.com/MarkoDojkic/SingiAttend_Multibranch_Jenkins.git',
                            credentialsId: 'github-creds'
                        ]]
                ])
            }
        }

        stage('Versioning') {
            when { expression { params.VERSION_ACTION != 'none' } }
            steps {
                script {
                    def currentVersion = readFile(env.VERSION_FILE).trim()
                    def nextVersion = ''

                    def versionCore = currentVersion.replace('-SNAPSHOT','')
                    def parts = versionCore.tokenize('.').collect { it.toInteger() }
                    if (parts.size() < 3) parts = [1,0,0]

                    switch(params.VERSION_ACTION) {
                        case 'bump-patch':
                            parts[2] = parts[2] + 1
                            nextVersion = "${parts[0]}.${parts[1]}.${parts[2]}-SNAPSHOT"
                            break
                        case 'bump-minor':
                            parts[1] = parts[1] + 1
                            parts[2] = 0
                            nextVersion = "${parts[0]}.${parts[1]}.${parts[2]}-SNAPSHOT"
                            break
                        case 'bump-major':
                            parts[0] = parts[0] + 1
                            parts[1] = 0
                            parts[2] = 0
                            nextVersion = "${parts[0]}.${parts[1]}.${parts[2]}-SNAPSHOT"
                            break
                        case 'release':
                            nextVersion = versionCore
                            break
                        default:
                            nextVersion = currentVersion
                            echo "No version change"
                    }

                    echo "New version: ${nextVersion}"
                    writeFile file: env.VERSION_FILE, text: newVersion
                    sh """
                        set +x

                        git -c user.name="Марко Дојкић" \
                            -c user.email="marko.dojkic@gmail.com" \
                            -c commit.gpgsign=true \
                            -c gpg.format=openpgp \
                            -c gpg.program="${GPG_SCRIPT}" \
                            commit -S -m "chore: bump version to ${nextVersion}"

                        set -x
                        git push origin HEAD:${env.BRANCH_NAME}
                    """
                }
            }
        }

        stage('Build Docker Image') {
            when { expression { !params.SKIP_DOCKER } }
            steps {
                script {
                    def versionTag = env.VERSION ?: readFile(env.VERSION_FILE).trim()
                    echo "Building Docker image ${env.DOCKER_IMAGE_NAME}:${versionTag}"

                    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh """
                            echo ${NEXUS_PASS} | docker login ${NEXUS_DOCKER_URL} -u ${NEXUS_USER} --password-stdin
                            docker pull ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:latest || true

                            DOCKER_BUILDKIT=1 docker build \
                                --cache-from ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:latest \
                                -t ${DOCKER_IMAGE_NAME}:${versionTag} \
                                -t ${DOCKER_IMAGE_NAME}:latest .
                        """
                    }
                }
            }
        }

        stage('Push Docker Image to Nexus') {
            when {
                allOf {
                    expression { !params.SKIP_DOCKER }
                    expression { !params.SKIP_DOCKER_DEPLOY }
                }
            }
            steps {
                script {
                    def versionTag = env.VERSION ?: readFile(env.VERSION_FILE).trim()
                    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh """
                            echo ${NEXUS_PASS} | docker login ${NEXUS_DOCKER_URL} -u ${NEXUS_USER} --password-stdin
                            docker tag ${DOCKER_IMAGE_NAME}:${versionTag} ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:${versionTag}
                            docker tag ${DOCKER_IMAGE_NAME}:latest ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:latest
                            docker push ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:${versionTag}
                            docker push ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:latest
                            docker logout ${NEXUS_DOCKER_URL}
                        """
                    }

                    sh """
                        docker rmi -f ${DOCKER_IMAGE_NAME}:${versionTag} \
                        ${DOCKER_IMAGE_NAME}:latest \
                        ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:${versionTag} \
                        ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:latest || true
                    """
                }
            }
        }

        stage('Sonar Analysis (FE)') {
            when {
                allOf {
                    expression { !params.SKIP_BUILD }
                    expression { params.RUN_SONAR }
                }
            }
            steps {
                script {
                    if (!params.SONAR_CREDENTIALS_ID?.trim()) {
                        error("SONAR_CREDENTIALS_ID is required when RUN_SONAR is enabled.")
                    }
                    def versionTag = env.VERSION ?: readFile(env.VERSION_FILE).trim()
                    withCredentials([string(credentialsId: '${SONAR_CREDENTIALS_ID}', variable: 'SONAR_TOKEN')]) {
                        sh """
                            sonar-scanner \
                                -Dsonar.projectKey=SingiAttend-Server-Jenkins \
                                -Dsonar.sources=www,Dockerfile,kubernetes.yaml \
                                -Dsonar.projectVersion=${versionTag} \
                                -Dsonar.host.url=${SONAR_URL} \
                                -Dsonar.token=${SONAR_TOKEN}
                        """
                    }
                }
            }
        }

        stage('Tag Build') {
            when { expression { params.TAG_THIS_BUILD } }
            steps {
                script {
                    def versionTag = env.VERSION ?: readFile(env.VERSION_FILE).trim()
                    sh """
                        git fetch --tags --force |
                        set +x
                        git -c user.name="Марко Дојкић" \
                            -c user.email="marko.dojkic@gmail.com" \
                            -c commit.gpgsign=true \
                            -c gpg.program="${GPG_SCRIPT}" \
                            -c gpg.format=openpgp \
                            tag -s \
                            -u ${GPG_KEY_ID} \
                            -m "Release singiattend/appstack-${versionTag} (branch: ${env.BRANCH_NAME})" \
                            singiattend-appstack-${versionTag}
                        set -x
                        echo "Verifying tag..."
                        git -c gpg.program="${GPG_SCRIPT}" verify-tag -v singiattend-appstack-${versionTag}
                        echo "Pushing tag..."
                        git push origin singiattend-appstack-${versionTag}
                    """
                }
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
        always {
            sh """
                set +x
                rm -rf "${JOB_TMP_FOLDER}"
                set -x
            """
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}
