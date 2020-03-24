import space.joserod.pipeline.PipelineManager
import space.joserod.configs.ProjectConfiguration

def call(PipelineManager pipelineManager){
    cleanWs()
    unstash "workspace"
    def projects = [:]
    pipelineManager.getProjectConfigurations().getProjectsConfigs().each{ k, v -> 
        def projectPath = v.path == null ? "" : v.path
        def projectName = v.name
         ProjectConfiguration projectConfiguration = pipelineManager.getProjectConfigurations().getProjectsConfigs().get(projectName)
        def tool = projectConfiguration.values.stages.build.tool
        def version = projectConfiguration.values.stages.build.version
        projects["${projectName}"] = {
            node("builder.ci.jenkins") {
            docker.image("${tool}:${version}").inside {
                stage("${projectName}") {
                    cleanWs()
                    unstash "workspace"
                    dir(projectPath) {
                        echo "${projectConfiguration.values.stages.build}"
                        if(tool.equals("node")) {
                            sh "${tool} --version"
                            sh "npm install"
                            sh "npm run build"
                            sh "ls -la"
                            stash name: "${projectPath}${tool}", includes: 'dist/**/*'
                        } else if (tool.equals("gradle")) {
                            sh "${tool} --version"
                            sh "gradle clean build"
                            sh "ls -la"
                            stash name: "${projectPath}${tool}", includes: 'build/**/**'
                        } else if (tool.equals("golang") ) {
                            if ( projectPath.equals("") ) {//we are in a unique situation we move current project into a folder
                            String envPath = "${env.GOPATH}"
                            echo envPath
                            // String commandToExectue = "mkdir -p "$env.GOPATH"/project && mv $(pwd)/* "$env.GOPATH"/project/"
                                // echo commandToExectue
                                // sh "ls -la "+"$env.GOPATH"+"\/project"
                            }
                            // withEnv(["GOPATH=$WORKSPACE", "GOBIN=$GOPATH/bin"]) {
                            //     sh "mkdir src bin && go get ./..."
                            //     env.PATH="${env.GOPATH}/bin:$PATH"
                            //     sh "go version"
                            //     sh "go get -d ./pkg/..."
                            //     sh "go install"
                            //     sh "go build -o ${projectName} main.go"
                            echo "${env.GOPATH}"
                                sh "ls -la"
                                error("exiting erarly")
                            // }
                        }
                        stash name: "${projectPath}${tool}docker", includes: 'dockerfiles/**'
                    }
                }
                }
            }
        }
    
    }
    parallel projects
    // error "Unstable, exiting now..."
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