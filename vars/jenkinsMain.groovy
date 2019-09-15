def call(){
pipeline {
    agent { label "builder.ci.jenkins"}
    stages {
        stage('build') {
            steps {
                script {
                    // echo env.JOB_NAME
                    // sh "mkdir -p $GOPATH/src/${env.JOB_NAME}"
                    // sh "ln -s $WORKSPACE $GOPATH/src/${env.JOB_NAME}"
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
