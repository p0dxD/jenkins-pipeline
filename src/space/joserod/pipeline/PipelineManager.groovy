package space.joserod.pipeline

import space.joserod.configs.Config

@Singleton
class PipelineManager {
    Config projectConfigurations
    boolean exitEarly

    void init() {
        projectConfigurations = Config.instance
    }

}