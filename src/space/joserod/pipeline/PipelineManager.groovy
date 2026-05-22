package space.joserod.pipeline

import space.joserod.configs.Config
import space.joserod.configs.ProjectConfiguration

public class PipelineManager {
    private Config configs
    private boolean exitEarly
    private String buildTag
    private String gitCommit
    private String gitAccount
    private String gitRepo
    private static PipelineManager pipelineManager = new PipelineManager()

    private PipelineManager() {}

    public static PipelineManager getInstance() {
        return pipelineManager
    }

    public void init() {
        configs = Config.getInstance()
        exitEarly = false
        buildTag = null
    }

    public Config getProjectConfigurations() {
        return this.configs
    }

    public void setExitEarly(boolean exitEarly) {
        this.exitEarly = exitEarly
    }

    public boolean exitEarly() {
        return this.exitEarly
    }

    public void setBuildTag(String tag) {
        this.buildTag = tag
    }

    public String getBuildTag() {
        return this.buildTag
    }

    public void setGitCommit(String sha) { this.gitCommit = sha }
    public String getGitCommit() { return this.gitCommit }

    public void setGitAccount(String account) { this.gitAccount = account }
    public String getGitAccount() { return this.gitAccount }

    public void setGitRepo(String repo) { this.gitRepo = repo }
    public String getGitRepo() { return this.gitRepo }

    public boolean hasFissionProjects() {
        return configs.getFissionProjects().size() > 0
    }

    public List<ProjectConfiguration> getFissionProjects() {
        return configs.getFissionProjects()
    }
}
