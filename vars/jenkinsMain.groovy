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
                    
            withEnv(["GOPATH=$WORKSPACE"]) {
                sh "mkdir src && go get ./..."
                env.PATH="${env.GOPATH}/bin:$PATH"
                    String repoName = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
                    // echo scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
                    // echo "WORSPACE: $WORKSPACE"
                    // sh "mkdir -p $GOPATH/src/${repoName} && (ln -s $WORKSPACE $GOPATH/src/${repoName} || true) && go get subway && echo $GOPATH && go build -o subway"
                    // sh "ln -s $WORKSPACE $GOPATH/src/${env.JOB_NAME}"
                    sh 'go build -o subway'
            
                    stash "workspace"
            }
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
