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
                    dir ("${projectPath}${tool}") {
                        unstash "${projectPath}${tool}"
                        unstash "${projectPath}${tool}docker"
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