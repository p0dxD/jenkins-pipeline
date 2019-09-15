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
        stage('Post Chechout') {
                agent { label "builder.ci.jenkins"}
            steps {
                script {
                    cleanWs()
                    unstash "workspace"
                     def resourceContent = libraryResource("scripts/post-checkout.sh")
                     echo "Contents: $resourceContent"
                    // sh "bash post-checkout.sh"

                    error "Finishing early"
                }
            }
        }
        stage('build') {
                agent { label "builder.ci.jenkins"}
            steps {
                script {
                    cleanWs()
                    unstash "workspace"
                    
            withEnv(["GOPATH=$WORKSPACE", "GOBIN=$GOPATH/bin"]) {
                sh "mkdir src bin && go get ./..."
                env.PATH="${env.GOPATH}/bin:$PATH"
                String repoName = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]

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
