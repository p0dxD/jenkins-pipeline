import space.joserod.pipeline.PipelineManager
def call(){
    PipelineManager pipelineManager = PipelineManager.getInstance();
    pipeline {
        agent none
        options {
            timestamps()
            skipDefaultCheckout()      // Don't checkout automatically
            disableConcurrentBuilds()
        }        
        stages {
            stage('Checkout') {          
                agent {
                    kubernetes {
                        cloud 'kubernetes'
                        inheritFrom 'kube-agent'
                        slaveConnectTimeout 300
                        idleMinutes 5
                    }    
                }
                steps {
                    script {
                        pipelineManager.init()// init pipeline configuration and manager
                        checkoutStage(pipelineManager)// initialize config, checkout code
                    }
                }
            }
            // stage('Post Chechout') {
            //     when {
            //         expression { !pipelineManager.exitEarly() }
            //     }  
            //     agent { label "builder.ci.jenkins"}
            //     steps {
            //         script {
            //             postCheckoutStage(pipelineManager)
            //         }
            //     }
            // }
            stage('build') {
                when {
                    expression { !pipelineManager.exitEarly() }
                }  
                agent {
                    kubernetes {
                        cloud 'kubernetes'
                        inheritFrom 'kube-agent'
                        slaveConnectTimeout 300
                        idleMinutes 5
                    }    
                }
                steps {
                    script {
                        buildStage(pipelineManager)
                    }
                }
            }
            stage('Create and push image') {
                when {
                    expression { !pipelineManager.exitEarly() }
                }  
                agent {
                    kubernetes {
                        cloud 'kubernetes'
                        inheritFrom 'kube-agent'
                        slaveConnectTimeout 300
                        idleMinutes 5
                    }    
                }
                steps {
                    script {
                        createImageStage(pipelineManager)
                    }
                }
            }
            // stage('Run image') {
            //     when {
            //         expression { !pipelineManager.exitEarly() && pipelineManager.getProjectConfigurations().getDockerConfigs().size() != 0 }
            //     }  
            //     agent { label "builder.ci.jenkins"}
            //     steps {
            //         script {
            //             runImageStage(pipelineManager)
            //         }
            //     }
            // }
        }
    }
}
