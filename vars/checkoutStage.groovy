import space.joserod.configs.Config

def call(Config configs) {
    cleanWs()
    checkout scm 
    def datas = readYaml file: 'configuration.yml'
    sh 'ls -la'
    echo "Dta: ${datas}"
    error "Unstable, exiting now..."
    stash "workspace"
}