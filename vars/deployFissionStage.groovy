import space.joserod.pipeline.PipelineManager

def call(PipelineManager pipelineManager) {
    def fissionProjects = pipelineManager.getFissionProjects()
    if (fissionProjects.isEmpty()) return

    podTemplate(yaml: """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: kubectl
    image: bitnami/kubectl:latest
    command: ["sleep"]
    args: ["99d"]
""") {
        node(POD_LABEL) {
            container('kubectl') {
                cleanBeforeCheckout()
                unstash 'workspace'

                fissionProjects.each { proj ->
                    def functionName = proj.values.function
                    echo "Deploying Fission function: ${functionName}"
                    sh "bash fission_functions/deploy.sh ${functionName}"
                }
            }
        }
    }
}
