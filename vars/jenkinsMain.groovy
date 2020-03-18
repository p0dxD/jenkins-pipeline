import space.joserod.configs.Config
def call(){
    Config configs = Config.getInstance();
    pipeline {
        agent none
        stages {
            stage('Checkout') {
                    agent { label "builder.ci.jenkins"}
                steps {
                    script {
                        checkoutStage(configs)//initialize config, checkout code
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
                        writeFile(file: "post-checkout.sh", text: resourceContent)
                        sh "bash post-checkout.sh"
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
                        build(configs)
                        error "Unstable, exiting now..."
                        withEnv(["GOPATH=$WORKSPACE", "GOBIN=$GOPATH/bin"]) {
                            sh "mkdir src bin && go get ./..."
                            env.PATH="${env.GOPATH}/bin:$PATH"
                            String repoName = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
                            sh "go get -d ./pkg/..."
                            sh "go install"
                            sh 'go build -o subway main.go'
                        
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
