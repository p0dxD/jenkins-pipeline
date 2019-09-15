def call(){
pipeline {
    agent none
    stages {
        stage('Checkout') {
                agent { label "builder.ci.jenkins"}
            steps {
                script {
                    cleanWs()
                    checkout scm
                    stash "workspace"
                }
            }
        }
        stage('build') {
                agent { label "builder.ci.jenkins"}
            steps {
                script {
                    cleanWs()
                    unstash "workspace"
                    echo env.JOB_NAME
                    sh "mkdir -p $GOPATH/src/${env.JOB_NAME} && ln -s $WORKSPACE $GOPATH/src/${env.JOB_NAME} && go build -o subway"
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
                    cleanWs()
                    unstash "workspace"
                    sh "cat ./docker/dockerize.sh"
                    sh './docker/dockerize.sh'
                }
            }
        }
    }
}
}
