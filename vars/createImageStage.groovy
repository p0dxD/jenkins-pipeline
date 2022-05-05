import space.joserod.pipeline.PipelineManager
import space.joserod.configs.ProjectConfiguration

def call(PipelineManager pipelineManager) {
    cleanWs()
    def projects = [:]
    pipelineManager.getProjectConfigurations().getProjectsConfigs().each{ k, v -> 
        def projectPath = v.path == null ? "" : v.path
        def projectName = v.name
         ProjectConfiguration projectConfiguration = pipelineManager.getProjectConfigurations().getProjectsConfigs().get(projectName)
        def stashName = projectConfiguration.values.stashName
        def version = projectConfiguration.values.version
        def configurationsToKeep = projectConfiguration.values.stages.build?.configuration
        String name = projectName.split("/").length > 1 ? projectName.split("/")[1] : projectName.split("/")[0]
        projects["${projectName}"] = {
        podTemplate(containers: [containerTemplate(name: 'kaniko', image: 'gcr.io/kaniko-project/executor:debug', command: '/busybox/cat', ttyEnabled: true)],
                    volumes: [persistentVolumeClaim(projectConfiguration.values.stages.build.volume),secretVolume(mountPath: '/root/.docker/', secretName: 'regcred')]) {
            node(POD_LABEL) {
                container('kaniko') {
                    stage('Creating image ' + name) {
                    cleanWs()
                    // dir ("${projectPath}${imageName}") {
                        getConfigurationFiles(name, projectPath, stashName, configurationsToKeep)
                        sh 'ls -la'
                        sh '/kaniko/executor --dockerfile=Dockerfile\
                                --destination=ghcr.io/p0dxd/joserod.space:latest \
                                --insecure \
                                --skip-tls-verify  \
                                -v=debug'

                        // getConfigurationFiles(name, projectPath, image, configurationsToKeep)
                        // sh "ls -la"
                        // String dockerfile = "Dockerfile"
                        // def customImage = docker.build("${projectName}","-f dockerfiles/${dockerfile} .")
                        // docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
                        //     customImage.push('latest')
                        // }
                    }
                }
                }
            
            }
        }
    
    }
    parallel projects
}

private void getConfigurationFiles(String name, String projectPath, String tool, def configurationsToKeep = null) {
    if ( projectPath.equals("") ) projectPath = "project"
    unstash "${projectPath}${tool}"
    unstash "${projectPath}${tool}docker"
    if ( configurationsToKeep != null ) {
        int index = 0
        for (String config : configurationsToKeep) {
            echo "Config: " + config
            unstash "${projectPath}${tool}${index}"
            index = index + 1
        }
    } 
}


// def label = "goweb-1.$BUILD_NUMBER-pipeline"
 
// podTemplate(label: label, containers: [
//  containerTemplate(name: 'kaniko', image: 'gcr.io/kaniko-project/executor:debug', command: '/busybox/cat', ttyEnabled: true)
// ],
// volumes: [
//    secretVolume(mountPath: '/root/.docker/', secretName: 'dockercred')
// ]) {
//  node(label) {
//    stage('Stage 1: Build with Kaniko') {
//      container('kaniko') {
//        sh '/kaniko/executor --context=git://github.com/repository/project.git \
//                --destination=docker.io/repository/image:tag \
//                --insecure \
//                --skip-tls-verify  \
//                -v=debug'
//      }
//    }
//  }
// }
