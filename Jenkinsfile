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
        stage('Checkout SCM') {
            steps {
                cleanWs() // 작업 공간 비우기
                echo "✅ Checking out source code from GitHub..."
                checkout scm
            }
        }

        stage('Build & Deploy only if on develop branch') {
            when {
                allOf {
                    branch 'develop' // develop 브랜치일 때만
                }
            }

            steps {
                echo "✅ Deploying develop branch build..."

                // Build the Docker image
                script {
                    sh '''
                    docker build -t $ECR_REPO:$IMAGE_TAG .
                    docker tag $ECR_REPO:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPO:$IMAGE_TAG
                    '''
                }

                // AWS ECR Login and Push Image
                script {
                    sh '''
                    aws ecr get-login-password --region $AWS_REGION \
                        | docker login --username AWS --password-stdin $ECR_REGISTRY

                    docker push $ECR_REGISTRY/$ECR_REPO:$IMAGE_TAG
                    '''
                }

                // SCP and SSH to EC2 to deploy
                script {
                    // 1. 네트워크가 없으면 생성 (이미 있으면 무시)
                    sh '''
                    ssh -i /var/lib/jenkins/.ssh/id_rsa ubuntu@$CHAT_EC2_IP "docker network create cluvr-net 2>/dev/null || echo 'Network already exists'"
                    '''

                    // 2. 의존성 서비스들이 실행 중인지 확인하고 없으면 시작
                    sh '''
                    ssh -i /var/lib/jenkins/.ssh/id_rsa ubuntu@$CHAT_EC2_IP "
                        # MongoDB 체크 및 시작
                        if [ ! \$(docker ps -q -f name=cluvr-mongo) ]; then
                            echo 'Starting MongoDB...'
                            docker run -d --name cluvr-mongo --network cluvr-net -p 27017:27017 --restart unless-stopped mongo:6.0
                        else
                            echo 'MongoDB already running'
                        fi

                        # MySQL 체크 및 시작
                        if [ ! \$(docker ps -q -f name=mysql) ]; then
                            echo 'Starting MySQL...'
                            docker run -d --name mysql --network cluvr-net -p 3307:3306 --restart unless-stopped \
                                -e MYSQL_DATABASE=cluvr-chat \
                                -e MYSQL_ROOT_PASSWORD=1100 \
                                -e MYSQL_ROOT_HOST='%' \
                                -e TZ=Asia/Seoul \
                                mysql:8.0
                        else
                            echo 'MySQL already running'
                        fi

                        # Redis 체크 및 시작
                        if [ ! \$(docker ps -q -f name=redis) ]; then
                            echo 'Starting Redis...'
                            docker run -d --name redis --network cluvr-net -p 6379:6379 --restart unless-stopped redis:7.2
                        else
                            echo 'Redis already running'
                        fi

                        # Zookeeper 체크 및 시작
                        if [ ! \$(docker ps -q -f name=zookeeper) ]; then
                            echo 'Starting Zookeeper...'
                            docker run -d --name zookeeper --network cluvr-net -p 2181:2181 --restart unless-stopped \
                                -e ZOOKEEPER_CLIENT_PORT=2181 \
                                -e ZOOKEEPER_TICK_TIME=2000 \
                                confluentinc/cp-zookeeper:7.5.0
                            sleep 5
                        else
                            echo 'Zookeeper already running'
                        fi

                        # Kafka 체크 및 시작
                        if [ ! \$(docker ps -q -f name=kafka) ]; then
                            echo 'Starting Kafka...'
                            docker run -d --name kafka --network cluvr-net -p 9092:9092 --restart unless-stopped \
                                -e KAFKA_BROKER_ID=1 \
                                -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
                                -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT \
                                -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://54.200.146.243:9092 \
                                -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
                                confluentinc/cp-kafka:7.5.0
                        else
                            echo 'Kafka already running'
                        fi
                    "
                    '''

                    // 3. Spring Boot 앱만 재시작 (매번 실행)
                    sh '''
                    ssh -i /var/lib/jenkins/.ssh/id_rsa ubuntu@$CHAT_EC2_IP "
                        echo 'Stopping existing Spring Boot app...'
                        docker stop cluvr-chat 2>/dev/null || true
                        docker rm cluvr-chat 2>/dev/null || true
                    "
                    '''

                    // 4. ECR 로그인 및 최신 이미지 pull
                    sh '''
                    ssh -i /var/lib/jenkins/.ssh/id_rsa ubuntu@$CHAT_EC2_IP "aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY"
                    ssh -i /var/lib/jenkins/.ssh/id_rsa ubuntu@$CHAT_EC2_IP "docker pull $ECR_REGISTRY/$ECR_REPO:$IMAGE_TAG"
                    '''

                    // 5. 새로운 Spring Boot 앱 시작
                    sh '''
                    ssh -i /var/lib/jenkins/.ssh/id_rsa ubuntu@$CHAT_EC2_IP "
                        echo 'Starting new Spring Boot app...'
                        docker run -d --name cluvr-chat --network cluvr-net -p 8082:8082 --restart unless-stopped $ECR_REGISTRY/$ECR_REPO:$IMAGE_TAG
                        echo 'Deployment completed!'
                    "
                    '''
                }}
            }
        }
    }
}