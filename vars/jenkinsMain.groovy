import space.joserod.pipeline.PipelineManager

def call() {
    /** The main configuration object. */
    PipelineManager pipelineManager = PipelineManager.instance

    pipeline {
        agent none
        options {
            skipDefaultCheckout()      // Don't checkout automatically
            disableConcurrentBuilds() // Only build one instance at a time
        }
        stages {
            /** Checks out the code and initializes the pipeline configuration. */
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
                        // pipelineManager.init()// init pipeline configuration and manager
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
        }
    }
}
