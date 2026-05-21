import space.joserod.pipeline.PipelineManager

def call(PipelineManager pipelineManager) {
    cleanBeforeCheckout()
    unstash 'workspace'

    pipelineManager.getProjectConfigurations().getProjectsConfigs().each { k, v ->
        if (v.values.type == 'fission') return

        def projectPath = v.values.path ?: '.'
        def stashName = v.values.name.replace('/', '_')

        dir(projectPath) {
            echo "Stashing ${v.values.name} from ${projectPath}"
            stash name: stashName, includes: '**/*', excludes: 'node_modules/**/*,.git/**'
        }
    }
}
