import space.joserod.pipeline.PipelineManager
import space.joserod.configs.ProjectConfiguration

def call(PipelineManager pipelineManager) {
  cleanBeforeCheckout()
  def projects = [:]
  pipelineManager.getProjectConfigurations().getProjectsConfigs().each { k, v ->
        def projectPath = v.path == null ? '' : v.path
        def projectName = v.name
    ProjectConfiguration projectConfiguration = pipelineManager.getProjectConfigurations().getProjectsConfigs().get(projectName)

        def framework = projectConfiguration.values.framework
        def stashName = (projectName + env.BRANCH_NAME).replace('/', '_')
        def version = projectConfiguration.values.version
        def configurationsToKeep = projectConfiguration.values.stages.build?.configuration
        String name = projectName.split('/').length > 1 ? projectName.split('/')[1] : projectName.split('/')[0]
        projects["${projectName}"] = {
      podTemplate(yaml: '''
              kind: Pod
              spec:
                containers:
                - name: kaniko
                  image: gcr.io/kaniko-project/executor:debug
                  imagePullPolicy: Always
                  command:
                  - sleep
                  args:
                  - 99d
                  volumeMounts:
                    - name: jenkins-docker-cfg
                      mountPath: /kaniko/.docker
                volumes:
                - name: jenkins-docker-cfg
                  projected:
                    sources:
                    - secret:
                        name: regcred
                        items:
                          - key: .dockerconfigjson
                            path: config.json
'''
  ) {
            node(POD_LABEL) {
          container(name: 'kaniko', shell: '/busybox/sh') {
            stage('Creating image ' + name) {
              cleanBeforeCheckout()
              // dir ("${projectPath}${imageName}") {
              def IMAGE_PUSH_DESTINATION = "${projectName}:${version}"
              getConfigurationFiles(name, projectPath, stashName, configurationsToKeep, framework)
              sh 'ls -la'
              sh "/kaniko/executor -f `pwd`/Dockerfile -c `pwd` --force --insecure --skip-tls-verify --cache=true --destination=ghcr.io/${projectName}:${version}"
                        // sh '/kaniko/executor --dockerfile=Dockerfile --verbosity=debug --destination="ghcr.io/p0dxD/joserod.space:latest"'

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

private void getConfigurationFiles(String name, String projectPath, String stashName, def configurationsToKeep = null, String framework = null) {
  if ( projectPath.equals('') ) projectPath = 'project'

  if (framework.equals('next')) {
    unstash name: "${stashName}package.json"
    unstash name: "${stashName}package_lock.json"
    unstash name: "${stashName}next_config"
    unstash name: "${stashName}public"
    unstash name: "${stashName}next"
    // unstash name: "${stashName}static"
    } else {
    unstash "${stashName}"
  }

  unstash "${stashName}docker"
  if ( configurationsToKeep != null ) {
    int index = 0
    for (String config : configurationsToKeep) {
      echo 'Config: ' + config
      unstash "${stashName}${index}"
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
