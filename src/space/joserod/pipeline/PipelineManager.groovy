package space.joserod.pipeline

import space.joserod.configs.Config

public class PipelineManager {
    private Config configs
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
}