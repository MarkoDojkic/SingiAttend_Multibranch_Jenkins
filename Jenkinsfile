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
        // Nexus / Sonar URLs
        NEXUS_RELEASE_URL = "http://localhost:8081/repository/maven-releases/"
        NEXUS_SNAPSHOT_URL = "http://localhost:8081/repository/maven-snapshots/"
        SONAR_URL = "http://localhost:9000"

        GPG_KEY_ID = "F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537"
    }

    parameters {
        booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run SonarQube analysis')
        choice(name: 'VERSION_ACTION', choices: ['none', 'bump-development', 'release'], description: 'Versioning action')
        booleanParam(name: 'TAG_THIS_BUILD', defaultValue: false, description: 'Create Git tag')
    }

    stages {
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
            steps {
                withMaven(maven: 'Maven 4.0', jdk: 'JDK 25') {
                    sh 'mvn clean install -DskipTests -B'
                }
            }
        }

        stage('Sonar Analysis') {
            when { expression { return params.RUN_SONAR == true } }
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

        stage('Tag Build') {
            when { expression { return params.TAG_THIS_BUILD == true } }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    def version = pom.version

                    withCredentials([string(credentialsId: 'gpg-passphrase', variable: 'GPG_PASS')]) {
                        sh """
                             git fetch --tags --force
                             git tag -d singiattend-server-${versionTag} 2>/dev/null || true
                             git push origin :refs/tags/singiattend-server-${versionTag} 2>/dev/null || true

                             printf "%s" "$GPG_PASS" | git -c gpg.passphrase-fd=0 tag -s -u "$GPG_KEY_ID" \
                                 -m "Release singiattend/server:${versionTag} (branch: ${BRANCH_NAME})" \
                                 singiattend-server-${versionTag}

                             git verify-tag -v singiattend-server-${versionTag}
                             git push origin singiattend-server-${versionTag}
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
    }
}