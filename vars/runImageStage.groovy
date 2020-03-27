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
        String name = projectName.split("/").length > 1 ? projectName.split("/")[1] : projectName.split("/")[0]
        projects["${projectName}"] = {
            node("builder.ci.jenkins") {
                stage("${projectName}") {
                    cleanWs()
                    echo "Docker configuration: " + pipelineManager.getProjectConfigurations().getDockerConfigs().get(projectName).values
                    LinkedHashMap dockerConfig = pipelineManager.getProjectConfigurations().getDockerConfigs().get(projectName).values
                    for (Map.Entry<String, ArrayList<String>> entry : dockerConfig.entrySet()) {
                        String key = entry.getKey();
                        ArrayList<String> value = entry.getValue();
                        for(String config : value) {
                            String command = config.command
                            String arguments = config.arguments
                            echo "command: ${command}" 
                            echo "arguments: ${arguments}"
                            withCredentials([string(credentialsId: 'remote_machine_secret', variable: 'mySecret')]) {
                                // some block can be a groovy block as well and the variable will be available to the groovy script

// previous_container="$(docker ps -aq --filter 'name=jenkins-website')"
// docker stop $previous_container
// docker rm $previous_container
                                sh """
                                    ssh $mySecret "
                                     source ~/.bashrc  
                                     docker pull $projectName:latest   
                                     docker ps -a  
                                     previous_container=`docker ps -aq --filter "name=$projectName"`
                                     echo Previous container: \$previous_container
                                     if [ -z \$previous_container ]; then echo "No container with that name."; else echo "Cleaning:." && docker stop \$previous_container && docker rm \$previous_container; fi 
                                     docker rmi `docker images -q`
                                     "
                                """
                            }
                        }
                    }

                }
            }
        }
    
    }
    parallel projects
}