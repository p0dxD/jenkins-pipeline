def call(){
pipeline {
    agent { label "builder.ci.jenkins"}
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
