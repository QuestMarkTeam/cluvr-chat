pipeline {
    agent any

    environment {
        AWS_REGION = 'us-west-2'
        AWS_ACCOUNT_ID = '617373894870'
        ECR_REPO = 'cluvr-chat'
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        IMAGE_TAG = 'latest'
        CHAT_EC2_IP = '54.200.146.243'
        ENV_PATH = '/home/ubuntu/cluvr-chat/env'
    }

    stages {
        stage('Create .env & Send to EC2') {
            steps {
                echo '✅ Generating .env and sending to EC2...'
                withCredentials([
                    string(credentialsId: 'JWT_SECRET_KEY', variable: 'JWT_SECRET_KEY'),
                    string(credentialsId: 'CHAT_DB_HOST', variable: 'MYSQL_HOST'),
                    string(credentialsId: 'CHAT_DB_PORT', variable: 'MYSQL_PORT'),
                    string(credentialsId: 'CHAT_DB_NAME', variable: 'MYSQL_DATABASE'),
                    string(credentialsId: 'CHAT_DB_USERNAME', variable: 'MYSQL_USERNAME'),
                    string(credentialsId: 'CHAT_DB_PASSWORD', variable: 'MYSQL_PASSWORD'),
                    string(credentialsId: 'CHAT_REDIS_HOST', variable: 'REDIS_HOST'),
                    string(credentialsId: 'CHAT_REDIS_PORT', variable: 'REDIS_PORT'),
                    string(credentialsId: 'USER_POOL_ID', variable: 'USER_POOL_ID'),
                    string(credentialsId: 'CLIENT_ID', variable: 'CLIENT_ID'),
                    string(credentialsId: 'CLIENT_SECRET', variable: 'CLIENT_SECRET'),
                    string(credentialsId: 'ACCESS_AWS', variable: 'ACCESS_AWS'),
                    string(credentialsId: 'SECRET_AWS', variable: 'SECRET_AWS'),
                    string(credentialsId: 'CHAT_MONGODB_HOST', variable: 'MONGODB_HOST'),
                    string(credentialsId: 'CHAT_MONGODB_PORT', variable: 'MONGODB_PORT'),
                    string(credentialsId: 'CHAT_MONGODB_DATABASE', variable: 'MONGODB_DATABASE'),
                    string(credentialsId: 'CHAT_KAFKA_BOOTSTRAP_SERVERS', variable: 'KAFKA_BOOTSTRAP_SERVERS'),
                    string(credentialsId: 'CHAT_KAFKA_CONSUMER_GROUP_ID', variable: 'KAFKA_CONSUMER_GROUP_ID'),
                    string(credentialsId: 'CHAT_KAFKA_CONSUMER_AUTO_OFFSET_RESET', variable: 'KAFKA_CONSUMER_AUTO_OFFSET_RESET'),
                    string(credentialsId: 'CHAT_EXTERNAL_CLUB_API_BASE_URL', variable: 'EXTERNAL_CLUB_API_BASE_URL'),
                    string(credentialsId: 'app_password', variable: 'app_password'),
                    string(credentialsId: 'CHAT_ZOOKEEPER_PORT', variable: 'ZOOKEEPER_PORT'),
                    string(credentialsId: 'CHAT_KAFKA_PORT', variable: 'KAFKA_PORT'),
                    string(credentialsId: 'CHAT_KAFKA_UI_PORT', variable: 'KAFKA_UI_PORT'),
                    string(credentialsId: 'CHAT_ECR_IMAGE_URI', variable: 'ECR_IMAGE_URI'),
                ]) {
                    sh """
                        echo "JWT_SECRET_KEY=${JWT_SECRET_KEY}" > .env
                        echo "MYSQL_HOST=${MYSQL_HOST}" >> .env
                        echo "MYSQL_PORT=${MYSQL_PORT}" >> .env
                        echo "MYSQL_DATABASE=${MYSQL_DATABASE}" >> .env
                        echo "MYSQL_USERNAME=${MYSQL_USERNAME}" >> .env
                        echo "MYSQL_PASSWORD=${MYSQL_PASSWORD}" >> .env
                        echo "REDIS_HOST=${REDIS_HOST}" >> .env
                        echo "REDIS_PORT=${REDIS_PORT}" >> .env
                        echo "USER_POOL_ID=${USER_POOL_ID}" >> .env
                        echo "CLIENT_ID=${CLIENT_ID}" >> .env
                        echo "CLIENT_SECRET=${CLIENT_SECRET}" >> .env
                        echo "ACCESS_AWS=${ACCESS_AWS}" >> .env
                        echo "SECRET_AWS=${SECRET_AWS}" >> .env
                        echo "MONGODB_HOST=${MONGODB_HOST}" >> .env
                        echo "MONGODB_PORT=${MONGODB_PORT}" >> .env
                        echo "MONGODB_DATABASE=${MONGODB_DATABASE}" >> .env
                        echo "KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS}" >> .env
                        echo "KAFKA_CONSUMER_GROUP_ID=${KAFKA_CONSUMER_GROUP_ID}" >> .env
                        echo "KAFKA_CONSUMER_AUTO_OFFSET_RESET=${KAFKA_CONSUMER_AUTO_OFFSET_RESET}" >> .env
                        echo "EXTERNAL_CLUB_API_BASE_URL=${EXTERNAL_CLUB_API_BASE_URL}" >> .env
                        echo "app_password=${app_password}" >> .env
                        echo "ZOOKEEPER_PORT=${ZOOKEEPER_PORT}" >> .env
                        echo "KAFKA_PORT=${KAFKA_PORT}" >> .env
                        echo "KAFKA_UI_PORT=${KAFKA_UI_PORT}" >> .env
                        echo "ECR_IMAGE_URI=${ECR_IMAGE_URI}" >> .env

                        scp -o StrictHostKeyChecking=no -i /var/lib/jenkins/.ssh/id_rsa .env ubuntu@${CHAT_EC2_IP}:${ENV_PATH}
                    """
                }
            }
        }

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

                    // 2. 의존성 서비스들이 실행 중인지 확인하고 없으면 시작 (이미 실행중이면 스킵)
                    sh '''
                    ssh -i /var/lib/jenkins/.ssh/id_rsa ubuntu@$CHAT_EC2_IP '
                        echo "🔍 의존성 서비스들 상태 확인 중..."

                        # MongoDB 체크 및 시작
                        if [ -z "$(docker ps -q -f name=cluvr-mongo)" ]; then
                            echo "📦 MongoDB 시작 중..."
                            docker run -d --name cluvr-mongo --network cluvr-net -p 27017:27017 --restart unless-stopped mongo:6.0
                        else
                            echo "✅ MongoDB 이미 실행 중 - 스킵"
                        fi

                        # MySQL 체크 및 시작
                        if [ -z "$(docker ps -q -f name=mysql)" ]; then
                            echo "📦 MySQL 시작 중..."
                            docker run -d --name mysql --network cluvr-net -p 3307:3306 --restart unless-stopped \
                                -e MYSQL_DATABASE=cluvr-chat \
                                -e MYSQL_ROOT_PASSWORD=1100 \
                                -e MYSQL_ROOT_HOST=% \
                                -e TZ=Asia/Seoul \
                                mysql:8.0
                        else
                            echo "✅ MySQL 이미 실행 중 - 스킵"
                        fi

                        # Redis 체크 및 시작
                        if [ -z "$(docker ps -q -f name=redis)" ]; then
                            echo "📦 Redis 시작 중..."
                            docker run -d --name redis --network cluvr-net -p 6379:6379 --restart unless-stopped redis:7.2
                        else
                            echo "✅ Redis 이미 실행 중 - 스킵"
                        fi

                        # Zookeeper 체크 및 시작
                        if [ -z "$(docker ps -q -f name=zookeeper)" ]; then
                            echo "📦 Zookeeper 시작 중..."
                            docker run -d --name zookeeper --network cluvr-net -p 2181:2181 --restart unless-stopped \
                                -e ZOOKEEPER_CLIENT_PORT=2181 \
                                -e ZOOKEEPER_TICK_TIME=2000 \
                                confluentinc/cp-zookeeper:7.5.0
                            sleep 5
                        else
                            echo "✅ Zookeeper 이미 실행 중 - 스킵"
                        fi

                        # Kafka 체크 및 시작
                        if [ -z "$(docker ps -q -f name=kafka)" ]; then
                            echo "📦 Kafka 시작 중..."
                            docker run -d --name kafka --network cluvr-net -p 9092:9092 --restart unless-stopped \
                                -e KAFKA_BROKER_ID=1 \
                                -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
                                -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT \
                                -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092 \
                                -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
                                confluentinc/cp-kafka:7.5.0
                        else
                            echo "✅ Kafka 이미 실행 중 - 스킵"
                        fi

                        echo "✅ 의존성 서비스들 준비 완료!"
                    '
                    '''

                    // 3. cluvr-chat 앱만 재시작 (매번 새로 배포)
                    sh '''
                    ssh -i /var/lib/jenkins/.ssh/id_rsa ubuntu@$CHAT_EC2_IP "
                        echo '🔄 기존 cluvr-chat 앱 중지 중...'
                        docker stop cluvr-chat 2>/dev/null || true
                        docker rm cluvr-chat 2>/dev/null || true
                        echo '✅ 기존 앱 정리 완료'
                    "
                    '''

                    // 4. ECR에서 최신 이미지 가져오기
                    sh '''
                    ssh -i /var/lib/jenkins/.ssh/id_rsa ubuntu@$CHAT_EC2_IP "
                        echo '🔐 ECR 로그인 중...'
                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY
                        echo '📥 최신 이미지 다운로드 중...'
                        docker pull $ECR_REGISTRY/$ECR_REPO:$IMAGE_TAG
                    "
                    '''

                    // 5. 새로운 cluvr-chat 앱 시작
                    sh '''
                    ssh -i /var/lib/jenkins/.ssh/id_rsa ubuntu@$CHAT_EC2_IP "
                        echo '🚀 새로운 cluvr-chat 앱 시작 중...'
                        docker run -d --name cluvr-chat --network cluvr-net -p 80:8082 --restart unless-stopped $ECR_REGISTRY/$ECR_REPO:$IMAGE_TAG
                        echo '🎉 배포 완료! 앱이 실행 중입니다.'
                        echo '📍 접속 주소: http://54.200.146.243'
                    "
                    '''
                }
            }
        }
    }
}
