pipeline {
    agent any

    environment {
        DOCKER_IMAGE       = 'vijay96000/calculator-api'
        DOCKER_CREDENTIALS = 'DockerHubCred'
        NOTIFICATION_EMAIL = 'vijay.vignesh96000@gmail.com'
        JAVA_HOME          = '/usr/lib/jvm/java-17-openjdk-amd64'
        PATH               = "${JAVA_HOME}/bin:${env.PATH}"
    }

    triggers {
        // GitHub webhook (preferred); Poll SCM is the fallback when the webhook is unreachable
        githubPush()
        pollSCM('H/2 * * * *')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        timeout(time: 20, unit: 'MINUTES')
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    docker.build("${DOCKER_IMAGE}:${BUILD_NUMBER}", '.')
                    sh "docker tag ${DOCKER_IMAGE}:${BUILD_NUMBER} ${DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Push') {
            steps {
                script {
                    docker.withRegistry('', "${DOCKER_CREDENTIALS}") {
                        sh "docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}"
                        sh "docker push ${DOCKER_IMAGE}:latest"
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                // `up -d` only recreates containers whose image/config changed, so re-runs
                // never tear down a healthy stack (no `compose down` here).
                sh 'IMAGE_TAG=${BUILD_NUMBER} docker compose up -d --remove-orphans'
                sh '''
                    for i in $(seq 1 24); do
                        if curl -sf http://localhost/actuator/health | grep -q '"status":"UP"'; then
                            echo "Deployment healthy"
                            curl -s "http://localhost/api/v1/add?a=2&b=3"; echo
                            docker compose ps
                            exit 0
                        fi
                        echo "Waiting for health check ($i/24)..."
                        sleep 5
                    done
                    docker compose ps
                    docker compose logs --tail=50 calculator-api
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            // keep the build-number tags small on disk; running containers keep their image
            sh 'docker image prune -f'
        }
        success {
            emailext(
                to: "${NOTIFICATION_EMAIL}",
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                body: """<p>Build <b>#${env.BUILD_NUMBER}</b> of <b>${env.JOB_NAME}</b> succeeded.</p>
                         <p>Image: ${DOCKER_IMAGE}:${env.BUILD_NUMBER} (pushed to Docker Hub and deployed with Docker Compose)</p>
                         <p>Console: <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>"""
            )
        }
        failure {
            emailext(
                to: "${NOTIFICATION_EMAIL}",
                subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                body: """<p>Build <b>#${env.BUILD_NUMBER}</b> of <b>${env.JOB_NAME}</b> failed.</p>
                         <p>Console: <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>""",
                attachLog: true
            )
        }
    }
}
