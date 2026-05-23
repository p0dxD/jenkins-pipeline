import space.joserod.pipeline.PipelineManager

def call(PipelineManager pipelineManager) {
    def dockerProjects = pipelineManager.getProjectConfigurations().getProjectsConfigs()
        .findAll { k, v -> v.values.type != 'fission' }
    if (dockerProjects.isEmpty()) return

    def buildTag = pipelineManager.getBuildTag()

    cleanBeforeCheckout()
    checkout scm

    // Group projects by their kustomization file to handle shared files (e.g. wellness/api + wellness/ui)
    def kustomizationMap = [:]
    dockerProjects.each { k, v ->
        def kPath = v.values.kustomization
        if (kPath == null) {
            echo "No kustomization path for ${v.values.name}, skipping tag bump"
            return
        }
        if (!kustomizationMap.containsKey(kPath)) {
            kustomizationMap[kPath] = readYaml(file: kPath)
        }
        def yaml = kustomizationMap[kPath]
        def imageName = v.values.image
        yaml.images.each { img ->
            if (img.name == imageName) {
                echo "Updating ${imageName} tag: ${img.newTag} -> ${buildTag}"
                img.newTag = buildTag
            }
        }
    }

    kustomizationMap.each { path, yaml ->
        writeYaml file: path, data: yaml, overwrite: true
    }

    withCredentials([usernamePassword(credentialsId: 'github-pat', usernameVariable: 'GH_USER', passwordVariable: 'GH_TOKEN')]) {
        sh """
            git config user.email "jenkins-ci@cube.local"
            git config user.name "Jenkins CI"
            ORIGIN=\$(git remote get-url origin)
            AUTHED=\$(echo "\$ORIGIN" | sed 's|https://|https://'\${GH_USER}':'\${GH_TOKEN}'@|')
            git remote set-url origin "\$AUTHED"
            git add -A
            if ! git diff --staged --quiet; then
                git commit -m "ci: bump image tags to ${buildTag} [skip ci]"
                git push origin HEAD:main
                git tag "v${buildTag}"
                git push origin "v${buildTag}"
                echo "Tags bumped and pushed. GitHub release tag v${buildTag} created."
            else
                echo "No tag changes to commit."
            fi
        """
    }
}
