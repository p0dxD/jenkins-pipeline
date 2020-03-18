package space.joserod.pipeline

import space.joserod.configs.Config

public class PipelineManager {
    private Config configs
    private static PipelineManager pipelineManager = new PipelineManager();

    private PipelineManager() {
    }

   //Get the only object available
   public static PipelineManager getInstance(){
      configs = Config.getInstance()
      return pipelineManager;
   }

    public void init() {
        configs = Config.getInstance()
    }

   private Config getConfigs() {
       return this.configs
   }
}