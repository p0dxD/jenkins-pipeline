import space.joserod.pipeline.PipelineManager


def call(PipelineManager pipelineManager){
    cleanWs()
    unstash "workspace"
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