pipeline {
    agent any

    options {
        timestamps()
    }

    environment {
        IMAGE_NAME = "mini-ecom-app"
        COMPOSE_FILE = "docker-compose.yml"
    }

    stages {
        stage("Checkout") {
            steps {
                checkout scm
            }
        }

        stage("CI - Verify") {
            steps {
                sh "chmod +x mvnw"
                withEnv([
                    "TESTCONTAINERS_RYUK_DISABLED=true",
                    "TESTCONTAINERS_CHECKS_DISABLE=true"
                ]) {
                    sh "./mvnw -B clean verify"
                }
            }
        }

        stage("Build Docker Image") {
            steps {
                sh "docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} -t ${IMAGE_NAME}:latest ."
            }
        }

        stage("CD - Deploy (main/master)") {
            when {
                branch "main"
            }
            steps {
                sh "APP_IMAGE=${IMAGE_NAME}:${BUILD_NUMBER} docker compose -f ${COMPOSE_FILE} --profile app up -d postgres app"
            }
        }
    }

    post {
        always {
            junit testResults: "target/surefire-reports/*.xml", allowEmptyResults: true
            archiveArtifacts artifacts: "target/cucumber-reports/*,target/site/jacoco/**", allowEmptyArchive: true
        }
    }
}
