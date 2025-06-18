pipeline {
    agent any

    environment {
        EC2_HOST = 'ubuntu@54.200.146.243'        // 너의 chat 서버 IP
        EC2_DIR = '/home/ubuntu/cluvr-chat'       // EC2 내부에서 git clone 받은 위치
    }

    stages {
        stage('Deploy to EC2') {
            steps {
                // chat 서비스만 재빌드 및 재시작 (다른 컨테이너는 유지)
                sh """
                ssh -o StrictHostKeyChecking=no -i /var/lib/jenkins/.ssh/id_rsa ${EC2_HOST} '
                cd ${EC2_DIR} &&
                git pull origin main &&
                docker-compose up -d --build cluvr-chat
                '
                """
            }
        }
    }
}