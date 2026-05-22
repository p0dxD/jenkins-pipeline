import space.joserod.pipeline.PipelineManager

def call(PipelineManager pipelineManager) {
    def fissionProjects = pipelineManager.getFissionProjects()
    if (fissionProjects.isEmpty()) return

    podTemplate(yaml: """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: deploy
    image: alpine/k8s:1.30.2
    command: ["sleep"]
    args: ["99d"]
""") {
        node(POD_LABEL) {
            container('deploy') {
                // Install fission CLI (match server v1.23.0) and zip
                sh """
                    apk add --no-cache zip --quiet
                    wget -qO /usr/local/bin/fission \
                        https://github.com/fission/fission/releases/download/v1.23.0/fission-v1.23.0-linux-arm64
                    chmod +x /usr/local/bin/fission
                """

                // Restart pool managers so auth tokens are fresh before any package build
                sh """
                    kubectl rollout restart deployment -n fission-function
                    kubectl rollout status deployment -n fission-function --timeout=120s
                """

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
