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
        GPG_KEY_ID = "F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537"
        GPG_HOME = "/var/lib/jenkins/.gnupg"
    }

    parameters {
        string(name: 'BE_VERSION', defaultValue: '', description: 'Backend JAR version (from BE build)')
        string(name: 'BE_JAR_SUFFIX', defaultValue: '', description: 'Backend JAR suffix')
        string(name: 'EUREKA_VERSION', defaultValue: '', description: 'Eureka JAR version (from Eureka build)')
        string(name: 'EUREKA_JAR_SUFFIX', defaultValue: '', description: 'Eureka JAR suffix')
        string(name: 'STUDENT_PROXY_VERSION', defaultValue: '', description: 'Student Proxy JAR version (from Student Proxy build)')
        string(name: 'STUDENT_PROXY_JAR_SUFFIX', defaultValue: '', description: 'Student Proxy JAR suffix')
        booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run SonarQube analysis')
        choice(name: 'VERSION_ACTION', choices: ['none', 'bump-development', 'release'], description: 'Versioning action')
        booleanParam(name: 'TAG_THIS_BUILD', defaultValue: false, description: 'Create Git tag')
        booleanParam(name: 'PUSH_TO_NEXUS', defaultValue: true, description: 'Push built Docker image to Nexus registry')
    }

    stages {
        stage('Checkout & Setup GPG') {
            steps {
                checkout scm
                script {
                    sh "git checkout -B ${env.BRANCH_NAME} origin/${env.BRANCH_NAME} || true"
                    
                    // Configure Git with GPG
                    sh """
                        # Configure Git user
                        git config --global user.name "Марко Дојкић"
                        git config --global user.email "marko.dojkic@gmail.com"
                        
                        # Configure GPG if available
                        if command -v gpg >/dev/null 2>&1; then
                            git config --global user.signingkey ${GPG_KEY_ID}
                            git config --global commit.gpgsign true
                            git config --global tag.gpgsign true
                            git config --global gpg.program \$(which gpg)
                            echo "GPG configured for commit and tag signing"
                        else
                            echo "Warning: GPG not available, commits and tags will not be signed"
                            git config --global commit.gpgsign false
                            git config --global tag.gpgsign false
                        fi
                    """
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
                    // Check if GPG is available
                    def gpgAvailable = sh(script: 'command -v gpg >/dev/null 2>&1', returnStatus: true) == 0
                    
                    // Commit with or without GPG signing
                    if (gpgAvailable) {
                        sh """
                            # Configure git user (using global settings from first stage)
                            git config --local user.name "Марко Дојкић"
                            git config --local user.email "marko.dojkic@gmail.com"
                            
                            # Add and commit changes with GPG signing
                            git add ${VERSION_FILE}
                            git commit -S -m 'Update FE IMAGE_TAG to ${newTag}' || echo 'no changes to commit'
                            git push origin HEAD:${env.BRANCH_NAME}
                        """
                    } else {
                        sh """
                            # Configure git user (using global settings from first stage)
                            git config --local user.name "Марко Дојкић"
                            git config --local user.email "marko.dojkic@gmail.com"
                            
                            # Add and commit changes without GPG signing
                            git add ${VERSION_FILE}
                            git -c commit.gpgsign=false commit -m 'Update FE IMAGE_TAG to ${newTag}' || echo 'no changes to commit'
                            git push origin HEAD:${env.BRANCH_NAME}
                        """
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    def versionTag = readFile(VERSION_FILE).readLines().find { it.startsWith('IMAGE_TAG=') }.split('=')[1].trim()

                    echo "Building Docker image with BE=${params.BE_JAR_SUFFIX}, Eureka=${params.EUREKA_JAR_SUFFIX}, Student Proxy=${params.STUDENT_PROXY_JAR_SUFFIX}"

                    withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh """
                            docker build \
                                --build-arg NEXUS_URL=http://host.lima.internal:8081 \
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
                        # Fetch all tags from remote
                        git fetch --tags --force
                        
                        # Configure git user (using global settings from first stage)
                        git config --local user.name "Марко Дојкић"
                        git config --local user.email "marko.dojkic@gmail.com"
                        
                        # Delete local tag if exists
                        git tag -d ${versionTag} 2>/dev/null || true
                        
                        # Delete remote tag if exists
                        git push origin :refs/tags/${versionTag} 2>/dev/null || true
                        
                        # For backward compatibility, also clean up any old branch-specific tags
                        git tag | grep "^${versionTag}-" | xargs -I {} git tag -d {}
                        git ls-remote --tags origin | grep "refs/tags/${versionTag}-" | awk '{print ":" \$2}' | xargs -I {} git push origin {} || true
                    """
                    
                    // Define tag name and message in Groovy
                    def tagName = versionTag
                    def tagMessage = "Version ${versionTag} (from branch: ${env.BRANCH_NAME})"
                    
                    sh """
                        # Create new signed or unsigned tag with previous format
                        if command -v gpg >/dev/null 2>&1; then
                            git tag -s '${tagName}' -m "${tagMessage}" || { echo "Failed to create signed tag"; exit 1; }
                        else
                            git -c tag.gpgsign=false tag ${tagName} -m "${tagMessage}" || { echo "Failed to create unsigned tag"; exit 1; }
                        fi
                        
                        # Push the new tag
                        git push origin '${tagName}' || { echo "Failed to push tag"; exit 1; }
                        
                        echo "Successfully created/updated tag '${tagName}' (version: ${versionTag} from branch: ${env.BRANCH_NAME})"
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