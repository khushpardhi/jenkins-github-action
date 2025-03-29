node {
    parameters {
        string(name: 'Imagetag', defaultValue: '1.0', description: 'this is imagetag')
    }
    try {
        stage('clone code') {
            cleanWs()  // Clean workspace before starting the job
            // Clone the Git repository
            git url: "https://github.com/khushpardhi/wanderlust.git", branch: "main"
        }

      stage("Login to DockerHub") {
            withCredentials([usernamePassword(credentialsId: "dockerHub", passwordVariable: "dockerHubPass", usernameVariable: "dockerHubUser")]) {
                sh "docker login -u ${env.dockerHubUser} -p ${env.dockerHubPass}"
            }
        }
        stage("Pull Docker Images to DockerHub") {
            def DOCKER_IMAGES = [
                "khushpardhi/backend:${params.Imagetag}",
                "khushpardhi/frontend:${params.Imagetag}"
            ]
            for (image in DOCKER_IMAGES) {
                echo "Pushing Docker image: ${image}"
                sh "docker pull ${image}"
            }
        }

       stage('Deploy') {
            script {
                def containersRunning = sh(script: "docker ps -q --filter 'label=com.docker.compose.project=wanderlust'", returnStdout: true).trim()
                if (containersRunning) {
                    echo "Containers are already running. Shutting them down and restarting..."
                    sh "docker-compose down"
                    sh "docker-compose up -d"
                } else {
                    echo "No containers are running. Starting the containers..."
                    sh "docker-compose up -d"
                }
            }
        }

    } catch (Exception e) {
        throw e
    } 
}
