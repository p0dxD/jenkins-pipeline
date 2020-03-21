import space.joserod.pipeline.PipelineManager
import space.joserod.configs.ProjectConfiguration

def call(PipelineManager pipelineManager){
    cleanWs()
    unstash "workspace"
    sh "ls -la"
    def projects = [:]
    pipelineManager.getProjectConfigurations().getProjectsConfigs().each{ k, v -> 
        println "${k}:${v.path}" 
        def projectPath = v.path
        def projectName = v.name
        projects["${projectName}"] = {
            node {
                agent {
                 docker { image 'node:7-alpine' }
                }
                stage("${projectName}") {
                    ProjectConfiguration projectConfiguration = pipelineManager.getProjectConfigurations().getProjectsConfigs().get(projectName)
                    echo "${projectConfiguration.values.stages.build}"
                     sh 'node --version'
                }
            }
        }
    
    }
    parallel projects
    error "Unstable, exiting now..."
    // withEnv(["GOPATH=$WORKSPACE", "GOBIN=$GOPATH/bin"]) {
    //     sh "mkdir src bin && go get ./..."
    //     env.PATH="${env.GOPATH}/bin:$PATH"
    //     String repoName = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
    //     sh "go get -d ./pkg/..."
    //     sh "go install"
    //     sh 'go build -o subway main.go'
    //     stash "workspace"
    // }
}