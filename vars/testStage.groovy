import space.joserod.pipeline.PipelineManager

def call(PipelineManager pipelineManager, String testType) {
    def projects = pipelineManager.getProjectConfigurations().getProjectsConfigs().values()
        .findAll { it.values?.stages?.test?."${testType}" }

    if (projects.isEmpty()) {
        echo "No changed projects have ${testType} tests — skipping."
        return
    }

    def branches = [:]

    projects.each { proj ->
        def projName    = proj.values.name
        def projPath    = proj.values.path ?: '.'
        def testConfig  = proj.values.stages.test."${testType}"
        def tool        = testConfig.tool ?: 'python'
        def testCommand = testConfig.command
        def image       = tool == 'node' ? 'node:20-alpine' : 'python:3.11-slim'

        branches["${projName}"] = {
            podTemplate(yaml: """
apiVersion: v1
kind: Pod
spec:
  serviceAccountName: jenkins-admin
  containers:
  - name: ${tool}
    image: ${image}
    command: ["sleep"]
    args: ["99d"]
""") {
                node(POD_LABEL) {
                    container(tool) {
                        stage("${testType}: ${projName}") {
                            cleanBeforeCheckout()
                            unstash 'workspace'
                            dir(projPath) {
                                sh testCommand
                            }
                        }
                    }
                }
            }
        }
    }

    parallel branches
}
