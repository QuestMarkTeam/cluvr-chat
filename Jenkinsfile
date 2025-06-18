pipeline {
    agent any

    environment {
        EC2_HOST = 'ubuntu@54.200.146.243'
        EC2_DIR = '/home/ubuntu/cluvr-chat'
    }

    stages {
        stage('Deploy to EC2') {
            steps {
                sh """
                ssh -o StrictHostKeyChecking=no ${EC2_HOST} << EOF
                  cd ${EC2_DIR}
                  git pull origin main
                  docker-compose up -d --build cluvr-chat
EOF
                """
            }
        }
    }
}