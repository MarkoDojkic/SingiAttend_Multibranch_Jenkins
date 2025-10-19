pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    environment {
        PATH = "/usr/local/bin:/opt/homebrew/bin:${env.PATH}"
        DOCKER_IMAGE_NAME = "singiattend-appstack"
        NEXUS_DOCKER_URL = "host.lima.internal:5001"
        VERSION_FILE = ".env"
    }

    parameters {
        string(name: 'BE_VERSION', defaultValue: '', description: 'Backend JAR version (from BE build)')
        string(name: 'BE_JAR_SUFIX', defaultValue: '', description: 'Backend JAR suffix')
        string(name: 'EUREKA_VERSION', defaultValue: '', description: 'Eureka JAR version (from Eureka build)')
        string(name: 'EUREKA_JAR_SUFIX', defaultValue: '', description: 'Eureka JAR suffix')
        booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run SonarQube analysis')
        choice(name: 'VERSION_ACTION', choices: ['none', 'bump-development', 'release'], description: 'Versioning action')
        booleanParam(name: 'TAG_THIS_BUILD', defaultValue: false, description: 'Create Git tag')
        booleanParam(name: 'PUSH_TO_NEXUS', defaultValue: true, description: 'Push built Docker image to Nexus registry')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    sh "git checkout -B ${env.BRANCH_NAME} origin/${env.BRANCH_NAME} || true"
                }
            }
        }

        stage('Prepare Version File') {
            steps {
                script {
                    if (!fileExists(VERSION_FILE)) {
                        echo "Creating default .env file..."
                        writeFile file: VERSION_FILE, text: """\
                            IMAGE_NAME=${DOCKER_IMAGE_NAME}
                            IMAGE_TAG=${params.BE_VERSION ?: '1.0.0-SNAPSHOT'}
                            IMAGE_LATEST_TAG=latest
                            NEXUS_URL=http://localhost:8081/repository/maven-releases
                        """
                    }
                    echo "Using version file:"
                    sh "cat ${VERSION_FILE}"
                }
            }
        }

        stage('Versioning') {
            when {
                expression { return params.VERSION_ACTION != 'none' }
            }
            steps {
                script {
                    // Read IMAGE_TAG safely
                    def imageTagLine = readFile(VERSION_FILE).readLines().find { it.startsWith('IMAGE_TAG=') }
                    def imageTag = imageTagLine ? imageTagLine.split('=')[1].trim() : '1.0.0-SNAPSHOT'

                    def newTag = imageTag

                    if (params.VERSION_ACTION == 'bump-development') {
                        // CPS-safe version bump without Matcher
                        def versionParts = imageTag.replace('-SNAPSHOT', '').split('\\.')
                        if (versionParts.size() >= 2) {
                            def major = versionParts[0].toInteger()
                            def minor = versionParts[1].toInteger() + 1
                            newTag = "${major}.${minor}.0-SNAPSHOT"
                            sh "sed -i '' 's/IMAGE_TAG=.*/IMAGE_TAG=${newTag}/' ${VERSION_FILE}"
                            echo "Bumped IMAGE_TAG to ${newTag}"
                        } else {
                            error "IMAGE_TAG '${imageTag}' is not in valid format (expected x.y.z[-SNAPSHOT])"
                        }
                    }

                    if (params.VERSION_ACTION == 'release') {
                        newTag = imageTag.replace('-SNAPSHOT', '-RC')
                        sh "sed -i '' 's/IMAGE_TAG=.*/IMAGE_TAG=${newTag}/' ${VERSION_FILE}"
                        echo "Set release IMAGE_TAG to ${newTag}"
                    }

                    // Commit version file
                    sh """
                        git config user.name "Марко Дојкић"
                        git config user.email "marko.dojkic@gmail.com"
                        git config user.signingkey F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537
                        git config commit.gpgsign true
                        git add ${VERSION_FILE}
                        git commit -S -m 'Update FE IMAGE_TAG to ${newTag}' || echo 'no commit needed'
                        git push origin HEAD:${env.BRANCH_NAME}
                    """
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    def beVersion = params.BE_VERSION ?: '1.0.0-SNAPSHOT'
                    def eurekaVersion = params.EUREKA_VERSION ?: '0.0.0'
                    def versionTag = readFile(VERSION_FILE).readLines().find { it.startsWith('IMAGE_TAG=') }.split('=')[1].trim()

                    echo "Building Docker image with BE=${beVersion}, Eureka=${eurekaVersion}"

                    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh """
                            docker build \
                                --build-arg NEXUS_URL=http://host.lima.internal:8081 \
                                --build-arg NEXUS_USER=\$NEXUS_USER \
                                --build-arg NEXUS_PASS=\$NEXUS_PASS \
                                --build-arg BE_JAR_VERSION=${beVersion} \
                                --build-arg BE_JAR_SUFIX=${params.BE_JAR_SUFIX} \
                                --build-arg EUREKA_JAR_VERSION=${eurekaVersion} \
                                --build-arg EUREKA_JAR_SUFIX=${params.EUREKA_JAR_SUFIX} \
                                -t ${DOCKER_IMAGE_NAME}:${versionTag} \
                                -t ${DOCKER_IMAGE_NAME}:latest .
                        """
                    }
                }
            }
        }

        stage('Push to Nexus') {
            when {
                expression { return params.PUSH_TO_NEXUS }
            }
            steps {
                script {
                    def versionTag = readFile(VERSION_FILE).readLines().find { it.startsWith('IMAGE_TAG=') }.split('=')[1].trim()
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
                expression { return params.RUN_SONAR }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'sonar-creds', usernameVariable: 'SONAR_USER', passwordVariable: 'SONAR_PASS')]) {
                    sh """
                        sonar-scanner \
                            -Dsonar.projectKey=SingiAttend-Server-Jenkins \
                            -Dsonar.sources=www \
                            -Dsonar.host.url=http://localhost:9000 \
                            -Dsonar.login=\$SONAR_PASS
                    """
                }
            }
        }

        stage('Tag Build') {
            when {
                expression { return params.TAG_THIS_BUILD }
            }
            steps {
                script {
                    def versionTag = readFile(VERSION_FILE).readLines().find { it.startsWith('IMAGE_TAG=') }.split('=')[1].trim()
                    echo "Creating Git tag: ${versionTag}"
                    sh """
                        git config user.name "Марко Дојкић"
                        git config user.email "marko.dojkic@gmail.com"
                        git config user.signingkey F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537
                        git tag -s ${versionTag} -m "Tag FE build ${versionTag}"
                        git push origin refs/tags/${versionTag}
                    """
                }
            }
        }

        stage('Cleanup') {
            steps {
                script {
                    def versionTag = readFile(VERSION_FILE).readLines().find { it.startsWith('IMAGE_TAG=') }.split('=')[1].trim()
                    echo "Cleaning up local Docker images..."
                    sh "docker rmi ${DOCKER_IMAGE_NAME}:${versionTag} || true"
                    sh "docker rmi ${DOCKER_IMAGE_NAME}:latest || true"
                    sh "docker image prune -f"
                }
            }
        }
    }

    post {
        success {
            echo "FE Docker build, optional Sonar analysis, and Nexus push completed successfully!"
        }
        failure {
            echo "FE pipeline failed!"
        }
    }
}