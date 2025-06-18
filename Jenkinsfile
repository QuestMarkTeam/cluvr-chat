pipeline {
  agent any

  environment {
    IMAGE_NAME = "cluvr-chat"
    PORT = "8082"
  }

  stages {
    stage('Build JAR') {
      steps {
        sh './gradlew clean build -x test'
      }
    }

    stage('Build Docker Image') {
      steps {
        sh 'docker build -t $IMAGE_NAME .'
      }
    }

    stage('Run Docker Container') {
      steps {
        sh '''
          docker stop $IMAGE_NAME || true
          docker rm $IMAGE_NAME || true
          docker run -d --name $IMAGE_NAME -p $PORT:$PORT $IMAGE_NAME
        '''
      }
    }
  }
}