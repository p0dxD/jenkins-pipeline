import space.joserod.pipeline.PipelineManager

// Builds EAS preview APKs for every mobile project and uploads them to local
// MinIO so they can be sideloaded without going through the app stores.
// Requires two Jenkins credentials (Secret Text):
//   - eas-token     : Expo personal access token
//   - minio-access  : MinIO root user / access key
//   - minio-secret  : MinIO root password / secret key
def call(PipelineManager pipelineManager) {
    def mobileProjects = pipelineManager.getProjectConfigurations().getProjectsConfigs()
        .findAll { k, v -> v.values.type == 'mobile' }

    if (!mobileProjects) {
        echo "No mobile projects found — skipping Mobile Publish stage."
        return
    }

    def minioEndpoint = 'http://192.168.50.77:9000'
    def minioBucket   = 'mobile-artifacts'

    def uploads = [:]
    mobileProjects.each { k, v ->
        def projectName = v.values.name
        def projectPath = v.values.path ?: '.'
        def stashName   = projectName.replace('/', '_')

        uploads["${projectName}"] = {
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
                        stage("Publish APK: ${projectName}") {
                            cleanBeforeCheckout()
                            unstash stashName

                            try {
                                withCredentials([
                                    string(credentialsId: 'eas-token',    variable: 'EXPO_TOKEN'),
                                    string(credentialsId: 'minio-access', variable: 'MINIO_ACCESS'),
                                    string(credentialsId: 'minio-secret', variable: 'MINIO_SECRET'),
                                ]) {
                                    dir(projectPath) {
                                        sh 'npm install -g eas-cli --quiet'

                                        def buildOutput = sh(
                                            script: "eas build --platform android --profile preview --non-interactive --json 2>/dev/null",
                                            returnStdout: true
                                        ).trim()

                                        def buildJson = readJSON text: buildOutput
                                        def builds    = buildJson instanceof List ? buildJson : [buildJson]

                                        // Install mc (MinIO Client)
                                        sh '''
                                            apk add --no-cache curl wget >/dev/null 2>&1 || true
                                            wget -q -O /usr/local/bin/mc https://dl.min.io/client/mc/release/linux-arm64/mc
                                            chmod +x /usr/local/bin/mc
                                            mc alias set local ''' + minioEndpoint + ''' ${MINIO_ACCESS} ${MINIO_SECRET} --quiet
                                            mc mb --ignore-existing local/''' + minioBucket + '''
                                        '''

                                        def slug = projectName.replace('/', '-').toLowerCase()

                                        builds.each { build ->
                                            def artifactUrl   = build?.artifacts?.buildUrl
                                            def buildPlatform = build?.platform?.toLowerCase() ?: 'android'

                                            if (artifactUrl && buildPlatform == 'android') {
                                                def filename = "${slug}-preview.apk"
                                                sh "wget -q -O '${filename}' '${artifactUrl}'"

                                                // Upload with a versioned path and overwrite the 'latest' pointer
                                                def buildTag = pipelineManager.getBuildTag() ?: env.BUILD_NUMBER
                                                sh """
                                                    mc cp '${filename}' local/${minioBucket}/${slug}/${buildTag}/${filename}
                                                    mc cp '${filename}' local/${minioBucket}/${slug}/latest/${filename}
                                                """

                                                echo "Published ${filename} → MinIO ${minioBucket}/${slug}/latest/"
                                                echo "Download: ${minioEndpoint}/${minioBucket}/${slug}/latest/${filename}"
                                            }
                                        }
                                    }
                                }
                            } catch (e) {
                                if (e.message?.contains('Could not find credentials')) {
                                    echo "Skipping Mobile Publish: credentials not configured yet (${e.message}). Add 'eas-token', 'minio-access', 'minio-secret' to Jenkins to enable."
                                    return
                                }
                                throw e
                            }
                        }
                    }
                }
            }
        }
    }

    parallel uploads
}
