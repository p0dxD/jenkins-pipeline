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
        def projName     = proj.values.name
        def projPath     = proj.values.path ?: '.'
        def testConfig   = proj.values.stages.test."${testType}"
        def tool         = testConfig.tool ?: 'python'
        def testCommand  = testConfig.command
        // Optional explicit image (e.g. a prebuilt test image with deps baked
        // in), otherwise default by tool.
        def image        = testConfig.image ?: (tool == 'node' ? 'node:20-alpine' : 'python:3.11-slim')
        // Optional result publishing + blocking behaviour.
        def junitPath    = testConfig.junit
        def coveragePath = testConfig.coverage
        def blocking     = (testConfig.blocking == null) ? true : testConfig.blocking

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
                                // Run the command but don't abort immediately on a
                                // non-zero exit — we still want to publish results
                                // so a failing test is visible on the build page.
                                def status = sh(script: testCommand, returnStatus: true)

                                if (junitPath) {
                                    junit testResults: junitPath, allowEmptyResults: true
                                }
                                if (coveragePath) {
                                    // Archive the Cobertura XML as a build artifact. We
                                    // avoid recordCoverage() here on purpose: when the
                                    // Coverage plugin isn't installed it throws a
                                    // java.lang.NoSuchMethodError (an Error, not an
                                    // Exception — uncatchable in the CPS sandbox), which
                                    // would fail the build. Once the Coverage plugin is
                                    // installed, switch this to recordCoverage for graphs.
                                    archiveArtifacts artifacts: coveragePath, allowEmptyArchive: true, fingerprint: false
                                }

                                if (status != 0) {
                                    if (blocking) {
                                        error("${projName} ${testType} failed (exit ${status})")
                                    } else {
                                        unstable("${projName} ${testType} reported issues (exit ${status}) — non-blocking")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    parallel branches
}
