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
        def image = projectConfiguration.values.stages.build.image
        // def version = projectConfiguration.values.stages.build.version
        def configurationsToKeep = projectConfiguration.values.stages.build?.configuration
        def framework = projectConfiguration.values.stages.build?.framework
        String name = projectName.split("/").length > 1 ? projectName.split("/")[1] : projectName.split("/")[0]
        //TODO: Move this to the configure stage/checkout
        def containerName = projectConfiguration.values.stages.build.container.name
        // def templateExample = containerTemplate(projectConfiguration.values.stages.build.container)
        // def templateExampleTwo = containerTemplate(name: 'python', image: 'python:latest', command: 'sleep', args: '30d')
        // def volume = persistentVolumeClaim(projectConfiguration.values.stages.build.volume)
        projects["${projectName}"] = {
            podTemplate(containers: [containerTemplate(projectConfiguration.values.stages.build.container)],
                        volumes: [persistentVolumeClaim(projectConfiguration.values.stages.build.volume)]) {
            node(POD_LABEL) {
                container(containerName) {
                    stage('Building ' + name + ' project') {
                        unstash "workspace"
                        dir(projectPath) {
                        echo "Doing node build."
                        sh '''
                        npm install
                        npm run build
                        '''
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
    if ( projectPath.equals("") ) projectPath = "project"
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
     stash name: "${projectPath}${tool}"//, excludes: 'node_modules/**/*'// it'll include all
}
