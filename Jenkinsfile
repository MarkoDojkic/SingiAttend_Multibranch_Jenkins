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
        SONAR_URL = "https://sonar.markodojkic.local"

        GPG_KEY_ID = "F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537"
        GPG_SCRIPT = "${JOB_TMP_FOLDER}/gpg-wrapper.sh"
        PATH = "/usr/local/bin:/usr/bin:${env.PATH}"

        DOCKER_IMAGE_NAME = "singiattend-server"
        NEXUS_DOCKER_URL = "nexus.markodojkic.local"
        DOCKER_BUILDKIT = '1'
    }

    parameters {
        booleanParam(name: 'SKIP_BUILD', defaultValue: false, description: 'Skip Maven build')
        booleanParam(name: 'SKIP_DEPLOY', defaultValue: false, description: 'Skip Maven deployment to Nexus')
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

        stage('Checkout Git project') {
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
            when { expression { params.VERSION_ACTION != 'none' } }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    def currentVersion = pom.version
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

                    withMaven(maven: 'Maven 4.0', jdk: 'JDK 26') {
                        sh """
                            mvn versions:set -B -DnewVersion=${nextVersion} -Dnexus.base.url=https://nexus.markodojkic.local -Dmaven.repo.local=${WORKSPACE}/.m2
                            mvn versions:commit -B -Dnexus.base.url=https://nexus.markodojkic.local -Dmaven.repo.local=${WORKSPACE}/.m2
                        """
                    }
                    sh """
                        git add pom.xml
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

        stage('Build') {
            when { expression { !params.SKIP_BUILD } }
            steps {
                withMaven(maven: 'Maven 4.0', jdk: 'JDK 26') {
                    sh "mvn clean install -DskipTests -U -B -Dnexus.base.url=https://nexus.markodojkic.local -Dmaven.repo.local=${WORKSPACE}/.m2"
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
                script {
                    if (!params.SONAR_CREDENTIALS_ID?.trim()) {
                        error("SONAR_CREDENTIALS_ID is required when RUN_SONAR is enabled.")
                    }
                    withCredentials([string(credentialsId: '${SONAR_CREDENTIALS_ID}', variable: 'SONAR_TOKEN')]) {
                        withMaven(maven: 'Maven 4.0', jdk: 'JDK 26') {
                            sh """
                                mvn -B sonar:sonar \
                                  -Dsonar.projectKey=SingiAttend-Server-Jenkins \
                                  -Dsonar.host.url=${SONAR_URL} \
                                  -Dsonar.token=${SONAR_TOKEN} \
                                  -Dnexus.base.url=https://nexus.markodojkic.local \
                                  -Dmaven.repo.local=${WORKSPACE}/.m2
                            """
                        }
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

                    withMaven(maven: 'Maven 4.0', jdk: 'JDK 26') {
                        sh "mvn deploy -B -Dnexus.base.url=https://nexus.markodojkic.local -Dmaven.repo.local=${WORKSPACE}/.m2"
                    }
                }
            }
        }

        stage('Build Docker Image') {
            when {
                allOf {
                    expression { !params.SKIP_BUILD }
                    expression { !params.SKIP_DOCKER }
                }
            }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    echo "Building Docker image ${env.DOCKER_IMAGE_NAME}:${pom.version}"

                    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh """
                            echo ${NEXUS_PASS} | docker login ${NEXUS_DOCKER_URL} -u ${NEXUS_USER} --password-stdin
                            docker pull ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:latest || true

                            DOCKER_BUILDKIT=1 docker build \
                                --cache-from ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:latest \
                                --build-arg VERSION=${pom.version} \
                                -t ${DOCKER_IMAGE_NAME}:${pom.version} \
                                -t ${DOCKER_IMAGE_NAME}:latest .
                        """
                    }
                }
            }
        }

        stage('Push Docker Image to Nexus') {
            when {
                allOf {
                    expression { !params.SKIP_BUILD }
                    expression { !params.SKIP_DOCKER }
                    expression { !params.SKIP_DOCKER_DEPLOY }
                }
            }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh """
                            echo ${NEXUS_PASS} | docker login ${NEXUS_DOCKER_URL} -u ${NEXUS_USER} --password-stdin
                            docker tag ${DOCKER_IMAGE_NAME}:${pom.version} ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:${pom.version}
                            docker tag ${DOCKER_IMAGE_NAME}:latest ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:latest
                            docker push ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:${pom.version}
                            docker push ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:latest
                            docker logout ${NEXUS_DOCKER_URL}
                        """
                    }

                    sh """
                        docker rmi -f ${DOCKER_IMAGE_NAME}:${pom.version} \
                        ${DOCKER_IMAGE_NAME}:latest \
                        ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:${pom.version} \
                        ${NEXUS_DOCKER_URL}/docker-colima-local/${DOCKER_IMAGE_NAME}:latest || true
                    """
                }
            }
        }

        stage('Tag Build') {
            when { expression { params.TAG_THIS_BUILD } }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
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
                            -m "Release singiattend/server:${pom.version} (branch: ${env.BRANCH_NAME})" \
                            singiattend-server-${pom.version}
                        set -x
                        echo "Verifying tag..."
                        git -c gpg.program="${GPG_SCRIPT}" verify-tag -v singiattend-server-${pom.version}
                        echo "Pushing tag..."
                        git push origin singiattend-server-${pom.version}
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