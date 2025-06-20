pipeline {
    agent any

    environment {
        AWS_REGION = 'us-west-2'
        AWS_ACCOUNT_ID = '617373894870'
        ECR_REPO = 'cluvr-chat'
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        IMAGE_TAG = 'latest'
        CHAT_EC2_IP = '54.200.146.243'
    }

    stages {
        stage('Build & Deploy only if on develop branch') {
            when {
                allOf {
                    branch 'develop'                  // develop 브랜치일 때만
                }
            }

            steps {
                echo "✅ Deploying develop branch build..."

                sh '''
                docker build -t $ECR_REPO:$IMAGE_TAG .
                docker tag $ECR_REPO:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPO:$IMAGE_TAG

                aws ecr get-login-password --region $AWS_REGION \
                    | docker login --username AWS --password-stdin $ECR_REGISTRY

                docker push $ECR_REGISTRY/$ECR_REPO:$IMAGE_TAG
                '''

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
