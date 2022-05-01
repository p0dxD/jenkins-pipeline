import space.joserod.pipeline.PipelineManager

def call(final PipelineManager pipelineManager) {
    cleanWs()
    checkout scm 
    fillconfiguration(pipelineManager)
    stash "workspace"
}

private void fillconfiguration(final PipelineManager pipelineManager) {
    //Read configuration
    def configuration = readYaml file: 'jenkinsconfig.yaml'
    configuration.each{ k, v -> 
        println "${k}:${v}"
        
     }
    // //Adding project configuration
    // for (Map.Entry<String, ArrayList<String>> entry : datas.entrySet()) {
    //     final String key = entry.getKey();
    //     final ArrayList<String> value = entry.getValue();
    //     for(String project : value) {
    //         //check the diffs if there isnt a change we can just return the pipeline, doesn't need to build.
    //         if (project.path == null) {
    //             pipelineManager.getProjectConfigurations().addProject(project.name, project)
    //             //docker configs
    //             addDockerConfiguration(pipelineManager, project.name)
    //             continue
    //         }
    //         String changesCmd = 'if [ '+"${project.path}" + ' != "." ] && [ -z $(git diff HEAD^ HEAD  --name-only | grep '+ "${project.path}" + ') ]; then echo "Empty"; else echo "Has changes."; fi'
    //         String changesCmdOutput = sh(script: changesCmd, returnStdout: true).trim()
    //         if (changesCmdOutput.equalsIgnoreCase('Has changes.')) {
    //             pipelineManager.getProjectConfigurations().addProject(project.name, project)
    //             echo "Added project with changes: " + project.name
    //             //docker configs
    //             addDockerConfiguration(pipelineManager, project.name, project.path)
    //         }
    //     }
    // }

    // if (pipelineManager.getProjectConfigurations().getProjectsConfigs().size() == 0) {
    //     //we exit pipeline there's no changes, why build? unless triggered by hand. 
    //     echo "We found one."
    //     pipelineManager.setExitEarly(true)// we want to skip other stages
    //     currentBuild.result = 'SUCCESS'
    //     return
    // }
}

private void addDockerConfiguration(PipelineManager pipelineManager, String projectName, String path = ".") {
    int checkforfile = sh(script: "[ -f ${path}/dockerfiles/dockerconfiguration.yml ]", returnStatus: true)
    echo "Status: ${checkforfile}"
    if ( checkforfile == 0) {
        LinkedHashMap dockerData = readYaml file: "${path}/dockerfiles/dockerconfiguration.yml"
        pipelineManager.getProjectConfigurations().addDockerConfig(projectName, dockerData)
    }
}