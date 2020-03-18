import space.joserod.configs.Config

def call(Config configs) {
    cleanWs()
    checkout scm 
    fillconfiguration(configs)
    stash "workspace"
}

private void fillconfiguration(Config configs) {
    //Read configuration
    LinkedHashMap datas = readYaml file: 'configuration.yml'

    for (Map.Entry<String, ArrayList<String>> entry : datas.entrySet()) {
        String key = entry.getKey();
        ArrayList<String> value = entry.getValue();
        for(String project : value) {
            //check the diffs if there isnt a change we can just return the pipeline, doesn't need to build.
            String changesCmd = 'if [ '+"${project.path}" + ' != "." ] && [ -z $(git diff HEAD^ HEAD  --name-only | grep '+ "${project.path}" + ') ]; then echo "Empty"; else echo "Has changes."; fi'
           String changesCmdOutput = sh(script: changesCmd, returnStdout: true).trim()
           if (changesCmdOutput.equalsIgnoreCase('Has changes.')) {
            configs.addProject(project.name, project)
            echo "Added project with changes: " + project.name
           }
        }
    }
    configs.getProjectsConfigs().each{ k, v -> println "${k}:${v.version}" }

    if (configs.getProjectsConfigs().size() == 1) {
        //we exit pipeline there's no changes, why build? unless triggered by hand. 
        currentBuild.result = 'SUCCESS'
        return
    }
}