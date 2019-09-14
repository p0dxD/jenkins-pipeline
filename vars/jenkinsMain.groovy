def call(){
pipeline {
    agent { label "docker.ci.jenkins"}
    stages {
        stage('build') {
            steps {
                script {
                    sh 'go version'
                }
            }
        }
    }
}
}
