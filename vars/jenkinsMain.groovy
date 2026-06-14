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
            stage('PR Build Validation') {
                when { beforeAgent true; expression { (env.BRANCH_NAME ?: 'main') != 'main' && !pipelineManager.exitEarly() } }
                agent {
                    kubernetes {
                        cloud 'kubernetes'
                        inheritFrom 'kube-agent'
                        slaveConnectTimeout 300
                        idleMinutes 5
                    }
                }
                steps {
                    script { prBuildStage(pipelineManager) }
                }
            }
            stage('Build') {
                when { beforeAgent true; expression { (env.BRANCH_NAME ?: 'main') == 'main' && !pipelineManager.exitEarly() } }
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
                when { beforeAgent true; expression { (env.BRANCH_NAME ?: 'main') == 'main' && !pipelineManager.exitEarly() } }
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
            stage('Mobile Build') {
                when { beforeAgent true; expression { (env.BRANCH_NAME ?: 'main') == 'main' && !pipelineManager.exitEarly() } }
                agent {
                    kubernetes {
                        cloud 'kubernetes'
                        inheritFrom 'kube-agent'
                        slaveConnectTimeout 300
                        idleMinutes 5
                    }
                }
                steps {
                    script { mobileBuildStage(pipelineManager) }
                }
            }
            stage('Mobile Publish') {
                when { beforeAgent true; expression { (env.BRANCH_NAME ?: 'main') == 'main' && !pipelineManager.exitEarly() } }
                agent {
                    kubernetes {
                        cloud 'kubernetes'
                        inheritFrom 'kube-agent'
                        slaveConnectTimeout 300
                        idleMinutes 5
                    }
                }
                steps {
                    script { mobilePublishStage(pipelineManager) }
                }
            }
            stage('Bump Tags') {
                when { beforeAgent true; expression { (env.BRANCH_NAME ?: 'main') == 'main' && !pipelineManager.exitEarly() } }
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
                        def notifyArgs = [
                            credentialsId: 'github-pat',
                            sha: pipelineManager.getGitCommit(),
                            account: pipelineManager.getGitAccount(),
                            repo: pipelineManager.getGitRepo(),
                            status: 'SUCCESS',
                            description: "Build #${env.BUILD_NUMBER} passed",
                        ]
                        githubNotify(notifyArgs + [context: 'Jenkins CI'])
                        githubNotify(notifyArgs + [context: 'continuous-integration/jenkins/pr-head'])
                    }
                }
                echo "Pipeline completed. Build tag: ${pipelineManager.getBuildTag() ?: 'n/a'}"
            }
            failure {
                script {
                    if (pipelineManager.getGitCommit()) {
                        def notifyArgs = [
                            credentialsId: 'github-pat',
                            sha: pipelineManager.getGitCommit(),
                            account: pipelineManager.getGitAccount(),
                            repo: pipelineManager.getGitRepo(),
                            status: 'FAILURE',
                            description: "Build #${env.BUILD_NUMBER} failed",
                        ]
                        githubNotify(notifyArgs + [context: 'Jenkins CI'])
                        githubNotify(notifyArgs + [context: 'continuous-integration/jenkins/pr-head'])
                    }
                }
            }
            unstable {
                // Report-only checks (e.g. security audit) can leave the build
                // UNSTABLE. Post SUCCESS so the commit status resolves (the
                // blocking gates passed) instead of hanging on PENDING — and so
                // a Renovate PR with a non-blocking advisory can still auto-merge.
                script {
                    if (pipelineManager.getGitCommit()) {
                        def notifyArgs = [
                            credentialsId: 'github-pat',
                            sha: pipelineManager.getGitCommit(),
                            account: pipelineManager.getGitAccount(),
                            repo: pipelineManager.getGitRepo(),
                            status: 'SUCCESS',
                            description: "Build #${env.BUILD_NUMBER} passed (non-blocking advisories)",
                        ]
                        githubNotify(notifyArgs + [context: 'Jenkins CI'])
                        githubNotify(notifyArgs + [context: 'continuous-integration/jenkins/pr-head'])
                    }
                }
            }
        }
    }
}
