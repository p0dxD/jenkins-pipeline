import space.joserod.configs.Config

// import java.util.LinkedHashMap
// import java.util.Map

def call(Config configs) {
    cleanWs()
    checkout scm 
    LinkedHashMap datas = readYaml file: 'configuration.yml'
    echo ""+datas.getClass()

    for (Map.Entry<String, ArrayList<String>> entry : datas.entrySet()) {
        String key = entry.getKey();
        ArrayList<String> value = entry.getValue();
        // now work with key and value...
        echo "Key:" + key
        echo "Value:" + value
    }
    sh 'ls -la'
    echo "Dta: ${datas}"
    error "Unstable, exiting now..."
    stash "workspace"
}