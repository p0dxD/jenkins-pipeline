import space.joserod.pipeline.PipelineManager

def call(PipelineManager pipelineManager) {
    def projects = [:]
    def buildTag = pipelineManager.getBuildTag()

    pipelineManager.getProjectConfigurations().getProjectsConfigs().each { k, v ->
        if (v.values.type == 'fission' || v.values.type == 'mobile') return

        def projectName = v.values.name
        def image = v.values.image
        def stashName = projectName.replace('/', '_')
        def buildArgs = v.values.stages?.docker?.build_args ?: [:]

        projects["${projectName}"] = {
            podTemplate(yaml: """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: buildctl
    image: moby/buildkit:v0.18.2
    command: ["sleep"]
    args: ["99d"]
""") {
                node(POD_LABEL) {
                    container('buildctl') {
                        stage("Image: ${projectName}") {
                            cleanBeforeCheckout()
                            unstash stashName

                            def buildArgFlags = buildArgs.collect { bk, bv ->
                                "--opt build-arg:${bk}=${bv}"
                            }.join(' ')

                            sh """
                                buildctl --addr tcp://buildkitd.devops-tools.svc.cluster.local:1234 \\
                                  build \\
                                  --frontend dockerfile.v0 \\
                                  --local context=. \\
                                  --local dockerfile=. \\
                                  --output type=image,name=${image}:${buildTag},push=true,registry.insecure=true \\
                                  ${buildArgFlags}
                            """
                            echo "Pushed ${image}:${buildTag}"
                        }
                    }
                }
            }
        }
    }

    parallel projects
}
