import space.joserod.pipeline.PipelineManager

// configPath: path to jenkinsconfig.yaml relative to repo root (default: 'jenkinsconfig.yaml')
def call(String configPath = 'jenkinsconfig.yaml') {
    PipelineManager pipelineManager = PipelineManager.getInstance()
    pipeline {
        agent none
        options {
            skipDefaultCheckout()
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
                        pipelineManager.init()
                        checkoutStage(pipelineManager, configPath)
                    }
                }
            }
            stage('Unit Tests') {
                when { beforeAgent true; expression { !pipelineManager.exitEarly() && pipelineManager.hasTestType('unit') } }
                agent {
                    kubernetes {
                        cloud 'kubernetes'
                        inheritFrom 'kube-agent'
                        slaveConnectTimeout 300
                        idleMinutes 5
                    }
                }
                steps {
                    script { unitTestStage(pipelineManager) }
                }
            }
            stage('Functional Tests') {
                when { beforeAgent true; expression { !pipelineManager.exitEarly() && pipelineManager.hasTestType('functional') } }
                agent {
                    kubernetes {
                        cloud 'kubernetes'
                        inheritFrom 'kube-agent'
                        slaveConnectTimeout 300
                        idleMinutes 5
                    }
                }
                steps {
                    script { functionalTestStage(pipelineManager) }
                }
            }
            stage('Integration Tests') {
                when { beforeAgent true; expression { !pipelineManager.exitEarly() && pipelineManager.hasTestType('integration') } }
                agent {
                    kubernetes {
                        cloud 'kubernetes'
                        inheritFrom 'kube-agent'
                        slaveConnectTimeout 300
                        idleMinutes 5
                    }
                }
                steps {
                    script { integrationTestStage(pipelineManager) }
                }
            }
            stage('Build') {
                when { expression { !pipelineManager.exitEarly() } }
                agent {
                    kubernetes {
                        cloud 'kubernetes'
                        inheritFrom 'kube-agent'
                        slaveConnectTimeout 300
                        idleMinutes 5
                    }
                }
                steps {
                    script { buildStage(pipelineManager) }
                }
            }
            stage('Build and Push Images') {
                when { expression { !pipelineManager.exitEarly() } }
                agent {
                    kubernetes {
                        cloud 'kubernetes'
                        inheritFrom 'kube-agent'
                        slaveConnectTimeout 300
                        idleMinutes 5
                    }
                }
                steps {
                    script { createImageStage(pipelineManager) }
                }
            }
            stage('Bump Tags') {
                when { expression { !pipelineManager.exitEarly() } }
                agent {
                    kubernetes {
                        cloud 'kubernetes'
                        inheritFrom 'kube-agent'
                        slaveConnectTimeout 300
                        idleMinutes 5
                    }
                }
                steps {
                    script { bumpTagsStage(pipelineManager) }
                }
            }
        }
        post {
            success {
                script {
                    if (pipelineManager.getGitCommit()) {
                        githubNotify credentialsId: 'github-pat',
                                     sha: pipelineManager.getGitCommit(),
                                     account: pipelineManager.getGitAccount(),
                                     repo: pipelineManager.getGitRepo(),
                                     status: 'SUCCESS',
                                     context: 'Jenkins CI',
                                     description: "Build #${env.BUILD_NUMBER} passed"
                    }
                }
                echo "Pipeline completed. Build tag: ${pipelineManager.getBuildTag() ?: 'n/a'}"
            }
            failure {
                script {
                    if (pipelineManager.getGitCommit()) {
                        githubNotify credentialsId: 'github-pat',
                                     sha: pipelineManager.getGitCommit(),
                                     account: pipelineManager.getGitAccount(),
                                     repo: pipelineManager.getGitRepo(),
                                     status: 'FAILURE',
                                     context: 'Jenkins CI',
                                     description: "Build #${env.BUILD_NUMBER} failed"
                    }
                }
            }
        }
    }
}
