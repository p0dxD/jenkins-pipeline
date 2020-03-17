import space.joserod.configs.Config

def call(space.joserod.configs.Config configs) {
    // cleanWs()
    // checkout scm 
    sh 'ls -la'
    error "Unstable, exiting now..."
    stash "workspace"
}