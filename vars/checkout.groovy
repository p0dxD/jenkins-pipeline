def call(Config configs) {
    cleanWs()
    checkout scm 
    sh 'ls -la'
    error "Unstable, exiting now..."
    stash "workspace"
}