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
           configs.addProject(project.name, project)
           echo "Added project: " + project.name
        }
    }
    configs.getProjects().each{ k, v -> println "${k}:${v}" }
    error "Unstable, exiting now..."
    stash "workspace"
}