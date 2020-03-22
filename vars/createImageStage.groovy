import space.joserod.pipeline.PipelineManager
import space.joserod.configs.ProjectConfiguration

def call(PipelineManager pipelineManager) {
    cleanWs()
    def projects = [:]
    pipelineManager.getProjectConfigurations().getProjectsConfigs().each{ k, v -> 
        println "${k}:${v.path}" 

        def projectPath = v.path
        def projectName = v.name
         ProjectConfiguration projectConfiguration = pipelineManager.getProjectConfigurations().getProjectsConfigs().get(projectName)
        def tool = projectConfiguration.values.stages.build.tool
        def version = projectConfiguration.values.stages.build.version
        projects["${projectName}"] = {
            node("builder.ci.jenkins") {
                stage("${projectName}") {
                    cleanWs()
                    unstash "${projectPath}${tool}"
                    sh "ls -la"
                    // dir(projectPath) {
                    //     echo "${projectConfiguration.values.stages.build}"
                    //     if(tool.equals("node")) {
                    //         sh "${tool} --version"
                    //         sh "npm install"
                    //         sh "npm run build"
                    //         sh "ls -la"
                    //         stash name: "${projectPath}${tool}", includes: 'dist/**/**'
                    //     } else if (tool.equals("gradle")) {
                    //         sh "${tool} --version"
                    //         sh "gradle clean build"
                    //         sh "ls -la"
                    //         stash name: "${projectPath}${tool}", includes: 'build/**/**'
                    //     }
                    // }
                }
            }
        }
    
    }
    parallel projects
    error "Unstable, exiting now..."
    // cleanWs()
    // unstash "workspace"
    // sh "cat ./docker/dockerize.sh"
    // sh './docker/dockerize.sh'
}