package space.joserod.pipeline

import space.joserod.configs.Config

@Singleton
class PipelineManager {
    Config projectConfigurations = Config.instance
    boolean exitEarly

    // void init() {
    //     projectConfigurations = Config.instance
    // }

}