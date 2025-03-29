node {
    parameters {
        string(name: 'Imagetag', defaultValue: '1.0', description: 'this is imagetag')
}
    try {
        stage("Clone Code") {
            cleanWs()
            git url: "https://github.com/khushpardhi/wanderlust.git", branch: "main"
        }

        stage("Login to DockerHub") {
            withCredentials([usernamePassword(credentialsId: "dockerHub", passwordVariable: "dockerHubPass", usernameVariable: "dockerHubUser")]) {
                sh "docker login -u ${env.dockerHubUser} -p ${env.dockerHubPass}"
            }
        }

         stage("Build Docker Images with Docker Compose") {
            echo "Building Docker images using docker-compose..."
            // Use the image tag parameter in the docker-compose build step
            sh "docker-compose build --build-arg IMAGE_TAG=${params.Imagetag}"
        }

        stage("Push Docker Images to DockerHub") {
            def DOCKER_IMAGES = [
                "khushpardhi/backend:${params.Imagetag}",
                "khushpardhi/frontend:${params.Imagetag}"
            ]
            for (image in DOCKER_IMAGES) {
                echo "Pushing Docker image: ${image}"
                sh "docker push ${image}"
            }
        }
      stage('Approval') {
            def userInput = input(
                message: 'Do you want to proceed with push on dockerhub', 
                parameters: [
                choice(name: 'Deploy', choices: ['Yes', 'No'], description: 'Do you want to deploy to the environment?')
                        ]       
            )
              if (userInput == 'Yes') {
                        build job: 'deploy_app', 
                            parameters: [
                                string(name: 'Imagetag', value: "${params.Imagetag}")
                            ]
                    } 
          
                }  
    } catch (Exception e) {
        currentBuild.result = "FAILURE"
        throw e
    }
}
