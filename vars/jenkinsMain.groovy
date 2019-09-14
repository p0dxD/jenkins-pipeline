def call(){
pipeline {
    agent { label "builder.ci.jenkins"}
    stages {
        stage('build') {
            steps {
                script {
                    sh 'go build -o subway'
                    stash "workspace"
                }
            }
        }
        stage('Create image') {
            steps {
                script {
                    unstash "workspace"
                    sh './docker/dockerize.sh'
                }
            }
        }
    }
}
}
