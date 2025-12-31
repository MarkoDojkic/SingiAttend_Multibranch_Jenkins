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
        DOCKER_IMAGE_NAME = "singiattend-mongo"
        NEXUS_DOCKER_URL = "markodojkic.local:5001"
        VERSION_FILE = "VERSION"
        DOCKER_BUILDKIT = '1'
        GPG_KEY_ID = "F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537"
        GPG_HOME = "${WORKSPACE}/.gnupg"
        PATH = "/usr/local/MacGPG2/bin:/usr/local/bin:/opt/homebrew/bin:/usr/bin:${env.PATH}"
    }

    parameters {
        booleanParam(name: 'SKIP_BUILD', defaultValue: false, description: 'Skip Docker build')
        booleanParam(name: 'SKIP_DEPLOY', defaultValue: false, description: 'Skip push to Nexus')
        choice(name: 'VERSION_ACTION', choices: ['none', 'bump-development', 'release'], description: 'Version bump or release')
        booleanParam(name: 'TAG_THIS_BUILD', defaultValue: false, description: 'Create signed Git tag')
    }

    stages {

        stage('Check Skip Options') {
            when { expression { params.SKIP_BUILD } }
            steps {
                echo "Build skipped as requested."
                script { currentBuild.result = 'SUCCESS' }
            }
        }

        stage('Setup GPG') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'gpg-secret-key', variable: 'GPG_KEY_FILE')]) {
                        sh '''
                            mkdir -p "$GPG_HOME"
                            chmod 700 "$GPG_HOME"

                            if [ ! -f "$GPG_HOME/pubring.kbx" ] || ! gpg --homedir "$GPG_HOME" --list-keys "${env.GPG_KEY_ID}" &>/dev/null; then
                                echo "Importing GPG key..."
                                gpg --batch --homedir "$GPG_HOME" --import "$GPG_KEY_FILE"
                                echo "''' + env.GPG_KEY_ID + ''':6:" | gpg --homedir "$GPG_HOME" --import-ownertrust
                            else
                                echo "GPG key ${env.GPG_KEY_ID} already exists, skipping import"
                            fi

                            echo "=== GPG Keys ==="
                            gpg --homedir "$GPG_HOME" --list-secret-keys --keyid-format LONG
                        '''
                        setupGitConfig()

                        sh '''
                            echo "=== Git GPG Config ==="
                            git config --global --list | grep -E "user|gpg|sign" || true
                        '''
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
                sh "git checkout -B ${env.BRANCH_NAME} origin/${env.BRANCH_NAME} || true"
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
                        sh """
                            git add ${env.VERSION_FILE}
                            git commit -S -m "chore: bump version to ${newVersion}" || echo "No changes"
                            git push origin HEAD:${env.BRANCH_NAME} || true
                        """
                        env.VERSION = newVersion
                    } else {
                        echo "No version change needed (current: ${currentVersion})"
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
                    echo "Building Docker image ${env.DOCKER_IMAGE_NAME}:${versionTag}"
                    sh """
                        DOCKER_BUILDKIT=1 docker build \
                            -t ${env.DOCKER_IMAGE_NAME}:${versionTag} \
                            -t ${env.DOCKER_IMAGE_NAME}:latest .
                    """
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

        stage('Tag Build') {
            when { expression { params.TAG_THIS_BUILD } }
            steps {
                script {
                    def versionTag = env.VERSION ?: readFile(env.VERSION_FILE).trim()
                    setupGitConfig()

                    echo "Creating signed tag singiattend-mongo-${versionTag}"
                    sh """
                        git fetch --tags --force
                        git tag -d singiattend-mongo-${versionTag} 2>/dev/null || true
                        git push origin :refs/tags/singiattend-mongo-${versionTag} 2>/dev/null || true

                        unset GIT_CONFIG_PARAMETERS
                        git config --local user.signingkey ${GPG_KEY_ID}

                        /usr/bin/git -c user.signingkey=${GPG_KEY_ID} tag \
                            -s -u ${GPG_KEY_ID} \
                            -m "Release singiattend/mongo:${versionTag} (branch: ${env.BRANCH_NAME})" \
                            singiattend-mongo-${versionTag}

                        git verify-tag -v singiattend-mongo-${versionTag}
                        git push origin singiattend-mongo-${versionTag}
                    """
                }
            }
        }

        stage('Cleanup') {
            steps {
                script {
                    def versionTag = env.VERSION ?: readFile(env.VERSION_FILE).trim()
                    echo "Cleaning up Docker artifacts..."
                    sh """
                        docker rmi ${env.DOCKER_IMAGE_NAME}:${versionTag} || true
                        docker rmi ${env.DOCKER_IMAGE_NAME}:latest || true
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ Build and deployment succeeded."
        }
        failure {
            echo "❌ Build or deployment failed!"
        }
        cleanup {
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}