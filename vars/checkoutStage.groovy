import space.joserod.pipeline.PipelineManager

def call(final PipelineManager pipelineManager, String configPath = 'jenkinsconfig.yaml') {
    cleanBeforeCheckout()
    checkout scm
    pipelineManager.setGitCommit(env.GIT_COMMIT)

    githubNotify credentialsId: 'github-pat',
                 status: 'PENDING',
                 context: 'Jenkins CI',
                 description: 'Build in progress'

    // Skip pipeline for commits made by the pipeline itself
    def commitMsg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
    if (commitMsg.contains('[skip ci]')) {
        pipelineManager.setExitEarly(true)
        currentBuild.result = 'SUCCESS'
        currentBuild.displayName = "#${env.BUILD_NUMBER} [skip ci]"
        echo "Skipping pipeline: commit contains [skip ci]"
        return
    }

    pipelineManager.setBuildTag("1.0.${env.BUILD_NUMBER}")
    echo "Build tag: ${pipelineManager.getBuildTag()}"

    fillConfiguration(pipelineManager, configPath)
    stash name: 'workspace', includes: '**/*', excludes: '.git/**'
}

private void fillConfiguration(final PipelineManager pipelineManager, String configPath) {
    def configuration = readYaml file: configPath
    def isTriggeredByUser = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').size() > 0

    for (def project : configuration.projects) {
        def projectPath = project.path ?: '.'
        String changesCmd = "if [ '${projectPath}' != '.' ] && [ -z \"\$(git diff HEAD^ HEAD --name-only | grep '${projectPath}')\" ]; then echo 'Empty'; else echo 'Has changes.'; fi"
        String changesOutput = sh(script: changesCmd, returnStdout: true).trim()

        if (changesOutput.equalsIgnoreCase('Has changes.') || isTriggeredByUser) {
            pipelineManager.getProjectConfigurations().addProject(project.name, project)
            echo "Queued for build: ${project.name}"
        } else {
            echo "No changes in ${projectPath}, skipping ${project.name}"
        }
    }

    if (pipelineManager.getProjectConfigurations().getProjectsConfigs().size() == 0) {
        echo "No changed projects found, exiting early."
        pipelineManager.setExitEarly(true)
        currentBuild.result = 'SUCCESS'
    }
}
