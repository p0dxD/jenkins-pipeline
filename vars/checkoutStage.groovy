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
    def isTriggeredByUser = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').size()
    configuration.each{ k, projects -> 
        for(String project : projects) {
            if (project.path == null) {
                pipelineManager.getProjectConfigurations().addProject(project.name, project)
                echo "Added project: " + project.name
                continue
            }
            echo "Adding project."
            String changesCmd = 'if [ '+"${project.path}" + ' != "." ] && [ -z $(git diff HEAD^ HEAD  --name-only | grep '+ "${project.path}" + ') ]; then echo "Empty"; else echo "Has changes."; fi'
            echo "Adding project 1."
            String changesCmdOutput = sh(script: changesCmd, returnStdout: true).trim()
            echo "Adding project 2."
            if (changesCmdOutput.equalsIgnoreCase('Has changes.') || isTriggeredByUser) {
                pipelineManager.getProjectConfigurations().addProject(project.name, project)
                echo "Added project with changes: " + project.name
            }
        }
     }

    if (pipelineManager.getProjectConfigurations().getProjectsConfigs().size() == 0) {
        //we exit pipeline there's no changes, why build? unless triggered by hand. 
        echo "We found one."
        pipelineManager.setExitEarly(true)// we want to skip other stages
        currentBuild.result = 'SUCCESS'
        return
    }
}

private void addDockerConfiguration(PipelineManager pipelineManager, String projectName, String path = ".") {
    int checkforfile = sh(script: "[ -f ${path}/dockerfiles/dockerconfiguration.yml ]", returnStatus: true)
    echo "Status: ${checkforfile}"
    if ( checkforfile == 0) {
        LinkedHashMap dockerData = readYaml file: "${path}/dockerfiles/dockerconfiguration.yml"
        pipelineManager.getProjectConfigurations().addDockerConfig(projectName, dockerData)
    }
}