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
        def configurationsToKeep = projectConfiguration.values.stages.build?.configuration
        def framework = projectConfiguration.values.stages.build?.framework
        String name = projectName.split("/").length > 1 ? projectName.split("/")[1] : projectName.split("/")[0]
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
                            saveConfigurationFiles(name, projectPath, tool, configurationsToKeep, framework)
                        } else if (tool.equals("gradle")) {
                            sh "${tool} --version"
                            sh "gradle clean build"
                            saveConfigurationFiles(name, projectPath, tool, configurationsToKeep)
                        } else if (tool.equals("golang") ) {
                            String newWorkspaceTmp = "${WORKSPACE}".replaceAll("@","_")
                            withEnv(["GOPATH=${newWorkspaceTmp}", "GOBIN=$GOPATH/bin", "PATH=$GOPATH/bin:$PATH"]) {
                                String envPath = "${env.GOPATH}"
                                //does not have a path
                                sh "mkdir -p ${envPath}"
                                if ( projectPath.equals("") ) {//we are in a unique situation we move current project into a folder
                                    sh "rm -Rf $envPath/project && mkdir -p $envPath/project && chmod a+rwx $envPath/project && mv \$(pwd)/* $envPath/project/"
                                    projectPath="project"
                                } else {
                                    sh "rm -Rf $envPath/$projectPath && mkdir -p $envPath/$projectPath && chmod a+rwx $envPath/$projectPath && mv \$(pwd)/$projectPath/* $envPath/$projectPath/"
                                } 
                                dir (envPath+"/"+projectPath) {// /home/go/{projectname}
                                    sh "mkdir src bin && go get ./..."
                                    sh "go version"
                                    int checkforPkgFolder = sh(script: "[ -d 'pkg' ]", returnStatus: true)
                                    if ( checkforPkgFolder == 0) {
                                        sh "go get -d ./pkg/..."
                                    }
                                    sh "go install"
                                    sh "go build -o ${name} main.go"

                                    saveConfigurationFiles(name, projectPath, tool, configurationsToKeep)
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    
    }
    parallel projects
}


private void saveConfigurationFiles(String projectName, String projectPath, String tool, def configurationsToKeep, String framework = null) {
    String name = projectName.split("/").length > 1 ? projectName.split("/")[1] : projectName.split("/")[0]
    if(tool.equals("node")) {
        if (framework != null) {
            configureForFrontendFramework(projectPath, tool, framework)
        } else {
            stash name: "${projectPath}${tool}", includes: 'dist/**/*'
        }
    } else if (tool.equals("gradle")) {
        stash name: "${projectPath}${tool}", includes: 'build/**/**'
    }  else if (tool.equals("golang") ) {
        stash name: "${projectPath}${tool}", includes: name
    }
    stash name: "${projectPath}${tool}docker", includes: 'dockerfiles/**'
    if ( configurationsToKeep != null ) {
        int index = 0
        for (String config : configurationsToKeep) {
            echo "Config: " + config
            stash name: "${projectPath}${tool}${index}", includes: config
            index = index + 1
        }
    } 
}

private void configureForFrontendFramework(String projectPath, String tool, String framework) {
     stash name: "${projectPath}${tool}", includes: './**/*', excludes: 'node_modules/**/*'// it'll include all
}