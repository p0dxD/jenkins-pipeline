import space.joserod.pipeline.PipelineManager

// Triggers Expo EAS cloud builds for every project with type: mobile.
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
        def platform   = v.values.stages?.mobile?.platform ?: 'all'
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
                                    def builds_list = buildJson instanceof List ? buildJson : [buildJson]

                                    builds_list.each { build ->
                                        def artifactUrl = build?.artifacts?.buildUrl
                                        def buildPlatform = build?.platform?.toLowerCase() ?: 'unknown'

                                        if (artifactUrl) {
                                            def ext = buildPlatform == 'ios' ? 'ipa' : 'aab'
                                            def filename = "jobsentry-${buildPlatform}.${ext}"
                                            sh "wget -q -O '${filename}' '${artifactUrl}'"
                                            archiveArtifacts artifacts: filename, fingerprint: true
                                            echo "Archived ${filename}"
                                        } else {
                                            echo "EAS ${buildPlatform} build submitted (async) — check expo.dev for artifact."
                                        }
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
