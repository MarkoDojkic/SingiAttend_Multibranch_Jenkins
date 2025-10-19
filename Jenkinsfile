pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    environment {
        NEXUS_RELEASE_URL = "http://localhost:8081/repository/maven-releases/"
        NEXUS_SNAPSHOT_URL = "http://localhost:8081/repository/maven-snapshots/"
        SONAR_URL = "http://localhost:9000"
        PATH = "/usr/local/bin:/opt/homebrew/bin:${env.PATH}" // Ensure gpg is on PATH
        GPG_EXECUTABLE = "/usr/local/bin/gpg" // adjust as needed
    }

    parameters {
        booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run SonarQube analysis')
        choice(name: 'VERSION_ACTION', choices: ['none', 'bump-development', 'release'], description: 'Versioning action')
        booleanParam(name: 'TAG_THIS_BUILD', defaultValue: false, description: 'Create Git tag')
    }

    stages {
        stage('Checkout & Setup GPG') {
            steps {
                script {
                    checkout scm

                    sh """
                        git checkout -B ${env.BRANCH_NAME} origin/${env.BRANCH_NAME} || true

                        # Configure Git GPG
                        if [ -x "$GPG_EXECUTABLE" ]; then
                            git config --global gpg.program $GPG_EXECUTABLE
                            git config --global user.signingkey F7CC88ED5C36404B4BA4B1FE039F7DC7EFE5D537
                            git config --global commit.gpgsign true
                        else
                            git config --global commit.gpgsign false
                        fi
                    """
                }
            }
        }

        stage('POM Info') {
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    echo "Project: ${pom.name}, Artifact: ${pom.artifactId}, Version: ${pom.version}"
                }
            }
        }

        stage('Versioning') {
            when { expression { params.VERSION_ACTION != 'none' } }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    def currentVersion = pom.version
                    def newVersion = currentVersion

                    if (params.VERSION_ACTION == 'bump-development') {
                        def parts = currentVersion.replace('-SNAPSHOT','').tokenize('.')
                        def major = parts[0].toInteger()
                        def minor = parts[1].toInteger() + 1
                        newVersion = "${major}.${minor}.0-SNAPSHOT"
                    } else if (params.VERSION_ACTION == 'release') {
                        newVersion = currentVersion.replace('-SNAPSHOT','')
                    }

                    echo "Setting version to: ${newVersion}"

                    withMaven(maven: 'Maven 4.0', jdk: 'JDK 17') {
                        sh """
                            mvn versions:set -DnewVersion=${newVersion}
                            mvn versions:commit
                        """
                    }

                    sh """
                        git add pom.xml
                        git commit -m 'Update version to ${newVersion}' || echo 'No changes to commit'
                        git push origin HEAD:${env.BRANCH_NAME}
                    """
                }
            }
        }

        stage('Build') {
            steps {
                withMaven(maven: 'Maven 4.0', jdk: 'JDK 17') {
                    sh 'mvn clean install -DskipTests -B'
                }
            }
        }

        stage('Sonar Analysis') {
            when { expression { params.RUN_SONAR } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'sonar-creds', usernameVariable: 'SONAR_USER', passwordVariable: 'SONAR_PASS')]) {
                    withMaven(maven: 'Maven 4.0', jdk: 'JDK 17') {
                        sh """
                            mvn sonar:sonar \
                              -Dsonar.projectKey=SingiAttend-Student_Proxy-Jenkins \
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
                    withMaven(maven: 'Maven 4.0', jdk: 'JDK 17') {
                        sh "mvn deploy -Dnexus.url=${deployUrl}"
                    }
                }
            }
        }

        stage('Tag Build') {
            when { expression { params.TAG_THIS_BUILD } }
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    def version = pom.version.replace('-SNAPSHOT','')

                    echo "Creating Git tag: ${version}"

                    sh """
                        # Remove local tag if it exists
                        git tag -d ${version} || true
                        git fetch --tags

                        if [ -x "$GPG_EXECUTABLE" ]; then
                            git tag -s ${version} -m "Tag version ${version}"
                        else
                            git -c tag.gpgSign=false tag ${version} -m "Tag version ${version}"
                        fi

                        git push origin refs/tags/${version} -f
                    """
                }
            }
        }

        stage('Cleanup') {
            steps {
                withMaven(maven: 'Maven 4.0', jdk: 'JDK 17') {
                    sh 'mvn clean -B'
                }
            }
        }
    }

    post {
        success { echo "✅ Build and deployment completed successfully!" }
        failure { echo "❌ Build failed!" }
    }
}
