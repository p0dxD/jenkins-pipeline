import space.joserod.configs.Config

def call() {
    // cleanWs()
    // checkout scm 
    sh 'ls -la'
    error "Unstable, exiting now..."
    stash "workspace"
}