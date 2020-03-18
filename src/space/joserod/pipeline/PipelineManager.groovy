package space.joserod.pipeline

import space.joserod.configs.Config

public class PipelineManager {
    private Config configs
    private static PipelineManager pipelineManager = new PipelineManager();

    private PipelineManager() {
        // configs = Config.getInstance()
    }

   //Get the only object available
   public static PipelineManager getInstance(){
      return pipelineManager;
   }

   private Config getConfigs() {
       return this.configs
   }
}