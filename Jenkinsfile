pipeline {
    agent any
    tools {
        jdk 'Java 8'
    }
    stages {
        stage ('Build') {
            steps {
                sh './gradlew clean && cd CubicChunksAPI && ./gradlew build && cd .. && ./gradlew build'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'build/libs/*.jar,CubicChunksAPI/build/libs/*.jar', fingerprint: true
                }
            }
        }
    }

    post {
        always {
            deleteDir()
        }
    }
}