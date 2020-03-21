import space.joserod.pipeline.PipelineManager
def call(){
    PipelineManager pipelineManager = PipelineManager.getInstance();
    pipeline {
        agent none
        options {
            timestamps()
            skipDefaultCheckout()      // Don't checkout automatically
        }        
        stages {
            stage('Checkout') {              
                agent { label "builder.ci.jenkins"}
                steps {
                    script {
                        pipelineManager.init()// init pipeline configuration and manager
                        checkoutStage(pipelineManager)// initialize config, checkout code
                    }
                }
            }
            stage('Post Chechout') {
                when {
                    expression { !pipelineManager.exitEarly() }
                }  
                agent { label "builder.ci.jenkins"}
                steps {
                    script {
                        postCheckoutStage(pipelineManager)
                    }
                }
            }
            stage('build') {
                when {
                    expression { !pipelineManager.exitEarly() }
                }  
                agent { label "master"}
                steps {
                    script {
                        buildStage(pipelineManager)
                    }
                }
            }
            stage('Create image') {
                when {
                    expression { !pipelineManager.exitEarly() }
                }  
                agent { label "builder.ci.jenkins"}
                steps {
                    script {
                        cleanWs()
                        unstash "workspace"
                        sh "cat ./docker/dockerize.sh"
                        sh './docker/dockerize.sh'
                    }
                }
            }
        }
    }
}
