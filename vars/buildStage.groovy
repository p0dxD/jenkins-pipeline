import space.joserod.pipeline.PipelineManager
import space.joserod.configs.ProjectConfiguration

def call(PipelineManager pipelineManager){
    cleanBeforeCheckout()
    unstash "workspace"
    def projects = [:]
    pipelineManager.getProjectConfigurations().getProjectsConfigs().each{ k, v -> 
        def projectPath = v.path == null ? "" : v.path
        def projectName = v.name
        ProjectConfiguration projectConfiguration = pipelineManager.getProjectConfigurations().getProjectsConfigs().get(projectName)
        // def image = projectConfiguration.values.stages.build.container.name
        def configurationsToKeep = projectConfiguration.values.stages.build?.configuration
        def framework = projectConfiguration.values.stages.build?.framework
        String name = projectName.split("/").length > 1 ? projectName.split("/")[1] : projectName.split("/")[0]
        def containerName = projectConfiguration.values.stages.build.tool
        def containerVersion = projectConfiguration.values.stages.build.version
        def stashName = (projectName+env.BRANCH_NAME).replace("/", "_")
        projects["${projectName}"] = {
            podTemplate(containers: [containerTemplate(name: containerName, image: "${containerName}:${containerVersion}")],
                        volumes: [persistentVolumeClaim(mountPath: "/root/${containerName}", claimName: "${containerName}", readOnly: false)]) {
            node(POD_LABEL) {
                container(containerName) {
                    stage('Building ' + name + ' project') {
                        unstash "workspace"
                        dir(projectPath) {
                            echo "In path : ${projectPath}"
                            sh "ls -la"
                            echo "Doing ${containerName} build."
                            def resourceContent = libraryResource("scripts/${containerName}.sh")
                            writeFile(file: "${containerName}.sh", text: resourceContent)
                            // sh "chmod +x ${containerName}.sh && ./${containerName}.sh || true"
                            // Stash configuration, and needed files
                            saveConfigurationFiles(projectName, projectPath, containerName, stashName)
                        }
                    }
                }
            }
            }
        }
    
    }
    parallel projects
}


private void saveConfigurationFiles(String projectName, String projectPath, String tool, String stashName, def configurationsToKeep = null, String framework = null) {
    echo "Will stash on: ${projectPath}${tool}"
    if ( projectPath.equals("") ) projectPath = "project"
    String name = projectName.split("/").length > 1 ? projectName.split("/")[1] : projectName.split("/")[0]
    if(tool.equals("node")) {
        if (framework != null) {
            configureForFrontendFramework(projectPath, stashName, framework)
        } else {
            sh "ls -la"
            stash name: "${stashName}", includes: 'dist/**/*'
        }
    } else if (tool.equals("gradle")) {
        stash name: "${stashName}", includes: 'build/**/**'
    }  else if (tool.equals("golang") ) {
        stash name: "${stashName}", includes: name
    }
    stash name: "${stashName}docker", includes: 'Dockerfile'
    if ( configurationsToKeep != null ) {
        int index = 0
        for (String config : configurationsToKeep) {
            echo "Config: " + config
            stash name: "${stashName}${index}", includes: config
            index = index + 1
        }
    } 
}

private void configureForFrontendFramework(String projectPath, String stashName, String framework) {
     stash name: "${stashName}"//, excludes: 'node_modules/**/*'// it'll include all
}
