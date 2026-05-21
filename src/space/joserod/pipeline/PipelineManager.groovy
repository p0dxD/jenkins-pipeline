package space.joserod.pipeline

import space.joserod.configs.Config
import space.joserod.configs.ProjectConfiguration

public class PipelineManager {
    private Config configs
    private boolean exitEarly
    private String buildTag
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

    public boolean hasFissionProjects() {
        return configs.getFissionProjects().size() > 0
    }

    public List<ProjectConfiguration> getFissionProjects() {
        return configs.getFissionProjects()
    }
}
