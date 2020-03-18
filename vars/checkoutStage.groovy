import space.joserod.configs.Config

// import java.util.LinkedHashMap
// import java.util.Map

def call(Config configs) {
    cleanWs()
    checkout scm 
    //Read configuration
    LinkedHashMap datas = readYaml file: 'configuration.yml'

    for (Map.Entry<String, ArrayList<String>> entry : datas.entrySet()) {
        String key = entry.getKey();
        ArrayList<String> value = entry.getValue();
        for(String project : value) {
            sh 'if [ '+"${project.path}"+' != "." ] && [ -z $(git diff HEAD^ HEAD  --name-only | grep '+"${project.path}"+') ]; then echo "Empty"; else echo "Has changes."; fi'
           configs.addProject(project.name, project)
           echo "Added project: " + project.name
        }
    }
    configs.getProjectsConfigs().each{ k, v -> println "${k}:${v.version}" }
    error "Unstable, exiting now..."
    stash "workspace"
}