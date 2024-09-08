import space.joserod.pipeline.PipelineManager

def call(final PipelineManager pipelineManager) {
    cleanBeforeCheckout()
    unstash 'workspace'
    def resourceContent = libraryResource('scripts/post-checkout.sh')
    writeFile(file: 'post-checkout.sh', text: resourceContent)
    sh 'bash post-checkout.sh'
    stash 'workspace'
}
