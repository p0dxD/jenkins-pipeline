package space.joserod.pipeline

import space.joserod.configs.Config

public class PipelineManager {
    private Config configs
    private boolean exitEarly
    private String dockerImageName
    private static PipelineManager pipelineManager = new PipelineManager();

    private PipelineManager() {
    }

   //Get the only object available
   public static PipelineManager getInstance(){
      return pipelineManager;
   }

    public void init() {
        configs = Config.getInstance()
    }

   private Config getProjectConfigurations() {
       return this.configs
   }

   public void setExitEarly(boolean exitEarly) {
       this.exitEarly = exitEarly
   }

   public boolean exitEarly() {
       return this.exitEarly
   }

   public void setDockerImageName(String dockerImageName) {
       this.dockerImageName = dockerImageName
   }

   public String getDockerImageName() {
       return this.dockerImageName
   }
}