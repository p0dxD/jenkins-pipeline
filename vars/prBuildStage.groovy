import space.joserod.pipeline.PipelineManager

// Builds every changed Docker image without pushing — used on PR branches to
// validate Dockerfiles and compile steps before merge to main.
def call(PipelineManager pipelineManager) {
    def projects = [:]

    pipelineManager.getProjectConfigurations().getProjectsConfigs().each { k, v ->
        if (v.values.type == 'fission' || v.values.type == 'mobile') return
        if (v.values.stages?.build?.tool == 'none') return

        def projectName = v.values.name
        def projectPath = v.values.path ?: '.'
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
                        stage("PR Build: ${projectName}") {
                            cleanBeforeCheckout()
                            unstash 'workspace'

                            def buildArgFlags = buildArgs.collect { bk, bv ->
                                "--opt build-arg:${bk}=${bv}"
                            }.join(' ')

                            dir(projectPath) {
                                sh """
                                    buildctl --addr tcp://buildkitd.devops-tools.svc.cluster.local:1234 \\
                                      build \\
                                      --frontend dockerfile.v0 \\
                                      --local context=. \\
                                      --local dockerfile=. \\
                                      --output type=tar,dest=/dev/null \\
                                      ${buildArgFlags}
                                """
                            }
                            echo "PR build validated (no push): ${projectName}"
                        }
                    }
                }
            }
        }
    }

    parallel projects
}
