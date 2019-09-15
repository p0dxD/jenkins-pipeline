def call(){
pipeline {
    agent { label none}
    stages {
        stage('build') {
                agent { label "builder.ci.jenkins"}
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
                agent { label "builder.ci.jenkins"}
            steps {
                script {
                    unstash "workspace"
                    sh "cat ./docker/dockerize.sh"
                    sh './docker/dockerize.sh'
                }
            }
        }
    }
}
}
