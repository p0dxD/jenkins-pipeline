import space.joserod.pipeline.PipelineManager

def call(final PipelineManager pipelineManager, String configPath = 'jenkinsconfig.yaml') {
    cleanBeforeCheckout()
    def scmVars = checkout scm
    def gitSha = scmVars.GIT_COMMIT ?: sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
    pipelineManager.setGitCommit(gitSha)
    env.GIT_PREVIOUS_SUCCESSFUL_COMMIT = scmVars.GIT_PREVIOUS_SUCCESSFUL_COMMIT ?: ''

    // Multibranch pipelines set BRANCH_NAME automatically; regular pipelines
    // triggered via webhook don't. Derive it from the checkout so the
    // branch-gating when-conditions in jenkinsMain work correctly.
    if (!env.BRANCH_NAME) {
        def rawBranch = scmVars.GIT_BRANCH ?: sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
        env.BRANCH_NAME = rawBranch.replaceAll(/^origin\//, '')
        echo "Derived BRANCH_NAME: ${env.BRANCH_NAME}"
    }

    def remoteUrl = sh(script: 'git remote get-url origin', returnStdout: true).trim()
    def repoPath = remoteUrl.replaceAll(/.*github\.com[:\/]/, '').replaceAll(/\.git$/, '')
    def parts = repoPath.split('/')
    pipelineManager.setGitAccount(parts[0])
    pipelineManager.setGitRepo(parts[1])

    def pendingArgs = [
        credentialsId: 'github-pat',
        sha: gitSha,
        account: parts[0],
        repo: parts[1],
        status: 'PENDING',
        description: 'Build in progress',
    ]
    githubNotify(pendingArgs + [context: 'Jenkins CI'])
    githubNotify(pendingArgs + [context: 'continuous-integration/jenkins/pr-head'])

    // Skip pipeline for commits made by the pipeline itself
    def commitMsg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
    if (commitMsg.contains('[skip ci]')) {
        pipelineManager.setExitEarly(true)
        currentBuild.result = 'SUCCESS'
        currentBuild.displayName = "#${env.BUILD_NUMBER} [skip ci]"
        echo "Skipping pipeline: commit contains [skip ci]"
        return
    }

    // Populate projectsConfigs before computing the tag so we can read kustomization files
    fillConfiguration(pipelineManager, configPath)

    if (!pipelineManager.exitEarly()) {
        def nextTag = computeNextTag(pipelineManager)
        pipelineManager.setBuildTag(nextTag)
        echo "Build tag: ${pipelineManager.getBuildTag()}"
        stash name: 'workspace', includes: '**/*', excludes: '.git/**'
    }
}

// Determine bump type from conventional commit message:
//   feat! / BREAKING CHANGE → major
//   feat:                   → minor
//   anything else           → patch
private String getBumpType() {
    def msg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
    if (msg.contains('BREAKING CHANGE') || msg =~ /^(\w+)!:/) return 'major'
    if (msg =~ /^feat(\(.+\))?:/) return 'minor'
    return 'patch'
}

// Find the highest semver tag across all kustomization images, then bump
// according to the conventional commit prefix in the triggering commit.
private String computeNextTag(PipelineManager pipelineManager) {
    def maxMajor = 1; def maxMinor = 0; def maxPatch = 0
    def seen = new HashSet()

    pipelineManager.getProjectConfigurations().getProjectsConfigs().each { k, v ->
        def kPath = v.values?.kustomization
        if (!kPath || seen.contains(kPath)) return
        seen.add(kPath)
        try {
            def yaml = readYaml(file: kPath)
            yaml.images?.each { img ->
                if (img.newTag) {
                    def p = img.newTag.toString().tokenize('.')
                    if (p.size() == 3) {
                        int maj = p[0] as int, min = p[1] as int, pat = p[2] as int
                        if (maj > maxMajor || (maj == maxMajor && min > maxMinor) ||
                            (maj == maxMajor && min == maxMinor && pat > maxPatch)) {
                            maxMajor = maj; maxMinor = min; maxPatch = pat
                        }
                    }
                }
            }
        } catch (e) {
            echo "Warning: could not read ${kPath} for tag derivation: ${e.message}"
        }
    }

    def bump = getBumpType()
    echo "Conventional commit bump type: ${bump}"
    switch (bump) {
        case 'major': return "${maxMajor + 1}.0.0"
        case 'minor': return "${maxMajor}.${maxMinor + 1}.0"
        default:      return "${maxMajor}.${maxMinor}.${maxPatch + 1}"
    }
}

// Files changed since the last successfully built commit. Diffing only
// HEAD^..HEAD misses earlier commits of a multi-commit push, silently
// skipping image builds for services those commits touched. Falls back to
// HEAD^ when Jenkins has no previous build or the commit was rewritten away,
// and to "everything changed" (null) on a repo's very first commit.
private List<String> getChangedFiles() {
    def baseline = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT
    if (!baseline || sh(script: "git cat-file -e ${baseline}^'{commit}' 2>/dev/null", returnStatus: true) != 0) {
        baseline = sh(script: 'git rev-parse --verify -q HEAD^ || true', returnStdout: true).trim()
    }
    if (!baseline) {
        echo 'No baseline commit found — treating all projects as changed'
        return null
    }
    echo "Detecting changes against baseline ${baseline}"
    def out = sh(script: "git diff --name-only ${baseline} HEAD", returnStdout: true).trim()
    return out ? out.split('\n') as List : []
}

private void fillConfiguration(final PipelineManager pipelineManager, String configPath) {
    def configuration = readYaml file: configPath
    def isTriggeredByUser = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').size() > 0
    def changedFiles = getChangedFiles()

    for (def project : configuration.projects) {
        def projectPath = project.path ?: '.'
        def hasChanges = (projectPath == '.') || (changedFiles == null)
        if (!hasChanges) {
            for (def file : changedFiles) {
                if (file == projectPath || file.startsWith(projectPath + '/')) {
                    hasChanges = true
                    break
                }
            }
        }

        if (hasChanges || isTriggeredByUser) {
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
