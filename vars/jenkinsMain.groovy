def call(){
pipeline {
    agent { label "builder.ci.jenkins"}
    stages {
        stage('build') {
            steps {
                script {
                    echo $JOB_NAME 
                    sh "mkdir -p $GOPATH/src/$JOB_NAME"
                    sh "ln -s $WORKSPACE $GOPATH/src/$JOB_NAME"
                    sh 'go get ./...'
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
