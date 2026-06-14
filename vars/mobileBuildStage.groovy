import space.joserod.pipeline.PipelineManager

// Triggers an Expo EAS cloud build for every project with type: mobile.
// Requires EXPO_TOKEN credential (Secret Text) with ID 'eas-token'.
def call(PipelineManager pipelineManager) {
    def mobileProjects = pipelineManager.getProjectConfigurations().getProjectsConfigs()
        .findAll { k, v -> v.values.type == 'mobile' }

    if (!mobileProjects) {
        echo "No mobile projects found — skipping Mobile Build stage."
        return
    }

    def builds = [:]
    mobileProjects.each { k, v ->
        def projectName = v.values.name
        def projectPath = v.values.path ?: '.'
        def platform   = v.values.stages?.mobile?.platform ?: 'android'
        def profile    = v.values.stages?.mobile?.profile   ?: 'production'
        def stashName  = projectName.replace('/', '_')

        builds["${projectName}"] = {
            podTemplate(yaml: """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: node
    image: node:20-alpine
    command: ["sleep"]
    args: ["99d"]
    resources:
      requests:
        memory: "512Mi"
        cpu: "250m"
""") {
                node(POD_LABEL) {
                    container('node') {
                        stage("Mobile Build: ${projectName}") {
                            cleanBeforeCheckout()
                            unstash stashName

                            withCredentials([string(credentialsId: 'eas-token', variable: 'EXPO_TOKEN')]) {
                                dir(projectPath) {
                                    sh 'npm install -g eas-cli --quiet'

                                    def buildOutput = sh(
                                        script: "eas build --platform ${platform} --profile ${profile} --non-interactive --json 2>/dev/null",
                                        returnStdout: true
                                    ).trim()

                                    def buildJson = readJSON text: buildOutput
                                    def artifactUrl = buildJson[0]?.artifacts?.buildUrl ?: buildJson?.artifacts?.buildUrl

                                    if (artifactUrl) {
                                        sh "wget -q -O jobsentry-android.aab '${artifactUrl}'"
                                        archiveArtifacts artifacts: 'jobsentry-android.aab', fingerprint: true
                                        echo "Android AAB archived: jobsentry-android.aab"
                                    } else {
                                        echo "EAS build submitted — artifact URL not available yet (async build). Check expo.dev for status."
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    parallel builds
}
