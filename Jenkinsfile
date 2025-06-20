pipeline {
    agent any

    environment {
        AWS_REGION = 'us-west-2'
        ECR_REGISTRY = '617373894870.dkr.ecr.us-west-2.amazonaws.com/cluvr-chat'
        ECR_REPO = 'cluvr-chat'
        IMAGE_TAG = 'latest'
        CHAT_EC2_IP = '54.200.146.243'
    }

    stages {
        stage('Build Docker Image') {
            steps {
                sh '''
                docker build -t $ECR_REPO:$IMAGE_TAG .
                docker tag $ECR_REPO:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPO:$IMAGE_TAG
                '''
            }
        }

        stage('Push to ECR') {
            steps {
                sh '''
                aws ecr get-login-password --region $AWS_REGION \
                  | docker login --username AWS --password-stdin $ECR_REGISTRY

                docker push $ECR_REGISTRY/$ECR_REPO:$IMAGE_TAG
                '''
            }
        }

        stage('Deploy to Chat EC2') {
            steps {
                sh """
                ssh -i /var/lib/jenkins/.ssh/id_rsa ubuntu@$CHAT_EC2_IP << 'EOF'
                docker-compose pull
                docker-compose up -d
                EOF
                """
            }
        }
    }
}