import space.joserod.pipeline.PipelineManager
import space.joserod.configs.ProjectConfiguration

def call(PipelineManager pipelineManager) {
    cleanWs()
    def projects = [:]
    pipelineManager.getProjectConfigurations().getProjectsConfigs().each{ k, v -> 
        def projectPath = v.path == null ? "" : v.path
        def projectName = v.name
         ProjectConfiguration projectConfiguration = pipelineManager.getProjectConfigurations().getProjectsConfigs().get(projectName)
        def tool = projectConfiguration.values.stages.build.tool
        def version = projectConfiguration.values.stages.build.version
        def configurationsToKeep = projectConfiguration.values.stages.build?.configuration
        projects["${projectName}"] = {
            node("builder.ci.jenkins") {
                stage("${projectName}") {
                    cleanWs()
                    dir ("${projectPath}${tool}") {
                        getConfigurationFiles(projectPath, tool, configurationsToKeep)
                        sh "ls -la"
                        String dockerfile = "Dockerfile"
                        def customImage = docker.build("${projectName}","-f dockerfiles/${dockerfile} .")
                        docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
                            customImage.push('latest')
                        }
                    }
                }
            }
        }
    
    }
    parallel projects
    // cleanWs()
    // unstash "workspace"
    // sh "cat ./docker/dockerize.sh"
    // sh './docker/dockerize.sh'
}

private void getConfigurationFiles(String projectPath, String tool, def configurationsToKeep) {
    unstash "${projectPath}${tool}"
    unstash "${projectPath}${tool}docker"
    if ( projectPath.equals("") ) projectPath = "project"
    if ( configurationsToKeep != null ) {
        int index = 0
        for (String config : configurationsToKeep) {
            echo "Config: " + config
            unstash "${projectPath}${tool}${index}"
            index = index + 1
        }
    } 
}