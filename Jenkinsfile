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
        PATH = "/usr/bin:/usr/local/bin/:${env.PATH}"
        DOCKER_IMAGE_NAME = "singiattend-appstack"
        NEXUS_DOCKER_URL = "markodojkic.local:5001"
        VERSION_FILE = "VERSION"
        DOCKER_BUILDKIT = '1'
        GPG_KEY_ID = "F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537"
    }

    parameters {
        string(name: 'BE_VERSION', defaultValue: '', description: 'Backend JAR version (from BE build)')
        string(name: 'BE_JAR_SUFFIX', defaultValue: '', description: 'Backend JAR suffix')
        string(name: 'EUREKA_VERSION', defaultValue: '', description: 'Eureka JAR version (from Eureka build)')
        string(name: 'EUREKA_JAR_SUFFIX', defaultValue: '', description: 'Eureka JAR suffix')
        string(name: 'STUDENT_PROXY_VERSION', defaultValue: '', description: 'Student Proxy JAR version (from Student Proxy build)')
        string(name: 'STUDENT_PROXY_JAR_SUFFIX', defaultValue: '', description: 'Student Proxy JAR suffix')
        booleanParam(name: 'SKIP_BUILD', defaultValue: false, description: 'Skip Docker build')
        booleanParam(name: 'SKIP_DEPLOY', defaultValue: false, description: 'Skip push to Nexus')
        booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run SonarQube analysis')
        choice(name: 'VERSION_ACTION', choices: ['none', 'bump-development', 'release'], description: 'Versioning action')
        booleanParam(name: 'TAG_THIS_BUILD', defaultValue: false, description: 'Create Git tag')
    }

    stages {
        stage('Checkout and Setup') {
            steps {
                checkout scm
                script {
                    // Ensure VERSION file exists
                    if (!fileExists(env.VERSION_FILE)) {
                        writeFile file: env.VERSION_FILE, text: '2.7.0-SNAPSHOT'
                        withCredentials([string(credentialsId: 'gpg-passphrase', variable: 'GPG_PASS')]) {
                            sh """
                                git add ${env.VERSION_FILE}
                                printf "%s" "${GPG_PASS}" | git commit -S -m "chore: initialize version file with 2.7.0-SNAPSHOT" || echo "No changes"
                                git push origin HEAD:${BRANCH_NAME} || true
                            """
                        }
                    }
                }
            }
        }

        stage('Version Management') {
            when { expression { params.VERSION_ACTION != 'none' } }
            steps {
                script {
                    def currentVersion = readFile(env.VERSION_FILE).trim()
                    def newVersion = ''

                    if (params.VERSION_ACTION == 'bump-development') {
                        def parts = currentVersion.replace('-SNAPSHOT','').tokenize('.')
                        if (parts.size() < 3) parts = [1,0,0]
                        def patch = parts[2].toInteger() + 1
                        newVersion = "${parts[0]}.${parts[1]}.${patch}-SNAPSHOT"
                    } else if (params.VERSION_ACTION == 'release') {
                        newVersion = currentVersion.replace('-SNAPSHOT','')
                    }

                    if (newVersion && newVersion != currentVersion) {
                        echo "Updating version from ${currentVersion} → ${newVersion}"
                        writeFile file: env.VERSION_FILE, text: newVersion
                        withCredentials([string(credentialsId: 'gpg-passphrase', variable: 'GPG_PASS')]) {
                            sh """
                                git add ${env.VERSION_FILE}
                                printf "%s" "${GPG_PASS}" | git commit -S -m "chore: bump version to ${newVersion}" || echo "No changes"
                                git push origin HEAD:${env.BRANCH_NAME} || true
                            """
                        }
                        env.VERSION = newVersion
                    } else {
                        env.VERSION = currentVersion
                    }
                }
            }
        }

        stage('Build Docker Image') {
            when { expression { !params.SKIP_BUILD } }
            steps {
                script {
                    def versionTag = env.VERSION ?: readFile(env.VERSION_FILE).trim()
                    echo "Building Docker image ${env.DOCKER_IMAGE_NAME}:${versionTag} with BE=${params.BE_JAR_SUFFIX}, Eureka=${params.EUREKA_JAR_SUFFIX}, Student Proxy=${params.STUDENT_PROXY_JAR_SUFFIX}"

                    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh """
                            echo \$NEXUS_PASS | docker login ${NEXUS_DOCKER_URL} -u \$NEXUS_USER --password-stdin
                            docker pull ${NEXUS_DOCKER_URL}/${DOCKER_IMAGE_NAME}:latest || true

                            DOCKER_BUILDKIT=1 docker build \
                                --cache-from ${NEXUS_DOCKER_URL}/${DOCKER_IMAGE_NAME}:latest \
                                --build-arg NEXUS_URL=https://markodojkic.local:8444 \
                                --build-arg NEXUS_USER=\$NEXUS_USER \
                                --build-arg NEXUS_PASS=\$NEXUS_PASS \
                                --build-arg BE_JAR_VERSION=${params.BE_VERSION} \
                                --build-arg BE_JAR_SUFFIX=${params.BE_JAR_SUFFIX} \
                                --build-arg EUREKA_JAR_VERSION=${params.EUREKA_VERSION} \
                                --build-arg EUREKA_JAR_SUFFIX=${params.EUREKA_JAR_SUFFIX} \
                                --build-arg STUDENT_PROXY_JAR_VERSION=${params.STUDENT_PROXY_VERSION} \
                                --build-arg STUDENT_PROXY_JAR_SUFFIX=${params.STUDENT_PROXY_JAR_SUFFIX} \
                                -t ${DOCKER_IMAGE_NAME}:${versionTag} \
                                -t ${DOCKER_IMAGE_NAME}:latest .
                        """
                    }
                }
            }
        }

        stage('Push to Nexus') {
            when { expression { !params.SKIP_DEPLOY } }
            steps {
                script {
                    def versionTag = env.VERSION ?: readFile(env.VERSION_FILE).trim()
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

        stage('Sonar Analysis (FE)') {
            when {
                allOf {
                    expression { !params.SKIP_BUILD }
                    expression { params.RUN_SONAR }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'sonar-creds', usernameVariable: 'SONAR_USER', passwordVariable: 'SONAR_PASS')]) {
                    sh """
                        sonar-scanner \
                            -Dsonar.projectKey=SingiAttend-Server-Jenkins \
                            -Dsonar.sources=www \
                            -Dsonar.host.url=https://markodojkic.local:8445 \
                            -Dsonar.login=${SONAR_PASS}
                    """
                }
            }
        }

        stage('Tag Build') {
            when { expression { params.TAG_THIS_BUILD } }
            steps {
                script {
                    def versionTag = env.VERSION ?: readFile(env.VERSION_FILE).trim()
                    withCredentials([string(credentialsId: 'gpg-passphrase', variable: 'GPG_PASS')]) {
                        sh """
                            git fetch --tags --force
                            git tag -d singiattend-appstack-${versionTag} 2>/dev/null || true
                            git push origin :refs/tags/singiattend-appstack-${versionTag} 2>/dev/null || true

                            printf "%s" "$GPG_PASS" | git -c gpg.passphrase-fd=0 tag -s -u "$GPG_KEY_ID" \
                                -m "Release singiattend/appstack:${versionTag} (branch: ${BRANCH_NAME})" \
                                singiattend-appstack-${versionTag}

                            git verify-tag -v singiattend-appstack-${versionTag}
                            git push origin singiattend-appstack-${versionTag}
                        """
                    }
                }
            }
        }

        stage('Cleanup') {
            steps {
                script {
                    def versionTag = env.VERSION ?: readFile(env.VERSION_FILE).trim()
                    echo "Cleaning up Docker artifacts..."
                    sh """
                        docker rmi ${DOCKER_IMAGE_NAME}:${versionTag} || true
                        docker rmi ${DOCKER_IMAGE_NAME}:latest || true
                    """
                }
            }
        }
    }

    post {
        success { echo "✅ Build and deployment succeeded." }
        failure { echo "❌ Build or deployment failed!" }
        cleanup { cleanWs(deleteDirs: true, notFailBuild: true) }
    }
}
