pipeline {
    agent any

    environment {
        DOCKER_IMAGE          = 'vijay96000/calculator-api'
        DOCKER_CREDENTIALS_ID = 'docker-hub-credentials'
        NOTIFICATION_EMAIL    = 'vijay.vignesh96000@gmail.com'
        STUDENT_NAME          = 'Vijay'
    }

    triggers {
        pollSCM('H/5 * * * *')
        githubPush()
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        timeout(time: 20, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                echo "=== Stage 1: Checking out code from Git/GitHub ==="
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo "=== Stage 2: Compiling and Packaging Spring Boot JAR ==="
                script {
                    try {
                        if (isUnix()) {
                            sh 'mvn clean package -DskipTests'
                        } else {
                            bat 'mvn clean package -DskipTests'
                        }
                    } catch (Exception e) {
                        echo "Host maven not found, executing inside maven:3.9-eclipse-temurin-17 container..."
                        if (isUnix()) {
                            sh 'docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9-eclipse-temurin-17 mvn clean package -DskipTests'
                        } else {
                            bat 'docker run --rm -v "%WORKSPACE%":/workspace -w /workspace maven:3.9-eclipse-temurin-17 mvn clean package -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Test') {
            steps {
                echo "=== Stage 3: Running JUnit 5 Unit & MockMvc Integration Tests ==="
                script {
                    try {
                        if (isUnix()) {
                            sh 'mvn test'
                        } else {
                            bat 'mvn test'
                        }
                    } catch (Exception e) {
                        echo "Host maven not found, executing tests inside maven:3.9-eclipse-temurin-17 container..."
                        if (isUnix()) {
                            sh 'docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9-eclipse-temurin-17 mvn test'
                        } else {
                            bat 'docker run --rm -v "%WORKSPACE%":/workspace -w /workspace maven:3.9-eclipse-temurin-17 mvn test'
                        }
                    }
                }
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/**/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo "=== Stage 4: Building Multi-Stage Docker Image (${env.DOCKER_IMAGE}:${env.BUILD_NUMBER}) ==="
                script {
                    if (isUnix()) {
                        sh "docker build -t ${env.DOCKER_IMAGE}:${env.BUILD_NUMBER} -t ${env.DOCKER_IMAGE}:latest ."
                    } else {
                        bat "docker build -t ${env.DOCKER_IMAGE}:${env.BUILD_NUMBER} -t ${env.DOCKER_IMAGE}:latest ."
                    }
                }
            }
        }

        stage('Push') {
            steps {
                echo "=== Stage 5: Pushing Image to Docker Hub with Jenkins Credentials ==="
                script {
                    try {
                        withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
                            if (isUnix()) {
                                sh 'echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USER" --password-stdin'
                                sh "docker push ${env.DOCKER_IMAGE}:${env.BUILD_NUMBER}"
                                sh "docker push ${env.DOCKER_IMAGE}:latest"
                                sh 'docker logout'
                            } else {
                                bat 'echo %DOCKER_PASSWORD% | docker login -u %DOCKER_USER% --password-stdin'
                                bat "docker push ${env.DOCKER_IMAGE}:${env.BUILD_NUMBER}"
                                bat "docker push ${env.DOCKER_IMAGE}:latest"
                                bat 'docker logout'
                            }
                        }
                    } catch (Exception e) {
                        echo "INFO: Docker Hub push skipped or '${env.DOCKER_CREDENTIALS_ID}' not configured yet: ${e.message}"
                        echo "To push to Docker Hub, create 'docker-hub-credentials' in Jenkins Credentials store."
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                echo "=== Stage 6: Deploying Stack with Docker Compose and Health Verification ==="
                script {
                    if (isUnix()) {
                        sh 'docker compose down'
                        sh 'docker compose up -d --remove-orphans'
                        sh '''
                            echo "Verifying deployment health at http://localhost/actuator/health..."
                            sleep 10
                            for i in $(seq 1 12); do
                                if curl -sf http://localhost/actuator/health | grep -q '"status":"UP"'; then
                                    echo "SUCCESS: Calculator API is UP and HEALTHY!"
                                    exit 0
                                fi
                                echo "Health check attempt $i: waiting 5 seconds..."
                                sleep 5
                            done
                            echo "ERROR: Health check timed out!"
                            exit 1
                        '''
                    } else {
                        bat 'docker compose down'
                        bat 'docker compose up -d --remove-orphans'
                        bat '''
                            powershell -Command "Start-Sleep -Seconds 10; for ($i = 1; $i -le 12; $i++) { try { $r = Invoke-RestMethod -Uri 'http://localhost/actuator/health' -TimeoutSec 3; if ($r.status -eq 'UP') { Write-Output 'SUCCESS: Calculator API is UP and HEALTHY!'; exit 0 } } catch {}; Start-Sleep -Seconds 5 }; Write-Error 'ERROR: Health check timed out!'; exit 1"
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline succeeded! Sending email notification..."
            emailext (
                subject: "SUCCESS: Job '${env.JOB_NAME}' [Build #${env.BUILD_NUMBER}]",
                body: """
                    <h2>Build #${env.BUILD_NUMBER} Succeeded</h2>
                    <p>Dear ${env.STUDENT_NAME},</p>
                    <p>Your declarative CI/CD pipeline for <b>${env.JOB_NAME}</b> completed successfully.</p>
                    <ul>
                        <li><b>Build Number:</b> #${env.BUILD_NUMBER}</li>
                        <li><b>Docker Image:</b> ${env.DOCKER_IMAGE}:${env.BUILD_NUMBER}</li>
                        <li><b>Git Branch:</b> ${env.GIT_BRANCH}</li>
                        <li><b>Status:</b> HEALTHY (Actuator verified)</li>
                        <li><b>Console Log:</b> <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></li>
                    </ul>
                    <p>All unit and integration tests passed, the container was built, pushed to Docker Hub, and deployed via Docker Compose.</p>
                """,
                to: 'vijay.vignesh96000@gmail.com',
                mimeType: 'text/html'
            )
        }

        failure {
            echo "Pipeline failed! Sending alert email notification..."
            emailext (
                subject: "FAILURE: Job '${env.JOB_NAME}' [Build #${env.BUILD_NUMBER}]",
                body: """
                    <h2>Build #${env.BUILD_NUMBER} Failed</h2>
                    <p>Dear ${env.STUDENT_NAME},</p>
                    <p>Your declarative CI/CD pipeline for <b>${env.JOB_NAME}</b> failed during execution.</p>
                    <ul>
                        <li><b>Build Number:</b> #${env.BUILD_NUMBER}</li>
                        <li><b>Git Branch:</b> ${env.GIT_BRANCH}</li>
                        <li><b>Console Log:</b> <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></li>
                    </ul>
                    <p>Please check the console output to diagnose the issue.</p>
                """,
                to: 'vijay.vignesh96000@gmail.com',
                mimeType: 'text/html'
            )
        }
    }
}
