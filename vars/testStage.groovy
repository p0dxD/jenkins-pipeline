import space.joserod.pipeline.PipelineManager

def call(PipelineManager pipelineManager, String testType) {
    def projects = pipelineManager.getProjectConfigurations().getProjectsConfigs().values()
        .findAll { it.values?.stages?.test?."${testType}" }

    if (projects.isEmpty()) {
        echo "No changed projects have ${testType} tests — skipping."
        return
    }

    podTemplate(yaml: """
apiVersion: v1
kind: Pod
spec:
  serviceAccountName: jenkins-admin
  containers:
  - name: python
    image: python:3.11-slim
    command: ["sleep"]
    args: ["99d"]
  - name: node
    image: node:20-alpine
    command: ["sleep"]
    args: ["99d"]
""") {
        node(POD_LABEL) {
            cleanBeforeCheckout()
            unstash 'workspace'
            projects.each { proj ->
                def testConfig = proj.values.stages.test."${testType}"
                def tool = testConfig.tool ?: 'python'
                echo "▶ ${testType} tests: ${proj.values.name}"
                container(tool) {
                    dir(proj.values.path) {
                        sh testConfig.command
                    }
                }
            }
        }
    }
}
