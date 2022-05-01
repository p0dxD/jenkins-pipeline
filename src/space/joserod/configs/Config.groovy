package space.joserod.configs

import java.util.ArrayList
import space.joserod.configs.ProjectConfiguration
import space.joserod.configs.DockerConfiguration

public class Config {
    private final HashMap<String, ProjectConfiguration> projects;
    // private final HashMap<String, DockerConfiguration> dockerConfigurations;
    private static Config config = new Config();

    private Config() {
        projects = new HashMap<>()
        // dockerConfigurations = new HashMap<>()
    }

   //Get the only object available
   public static Config getInstance(){
      return config;
   }

    public void addProject(String name, LinkedHashMap values) {
        ProjectConfiguration tmp = new ProjectConfiguration(values)
        projects.put(name, tmp)
    }

    public void addProject(String name, ProjectConfiguration project) {
        projects.put(name, tmp)
    }
    // public void addDockerConfig(String name, LinkedHashMap values) {
    //     DockerConfiguration tmp = new DockerConfiguration(values)
    //     dockerConfigurations.put(name, tmp)
    // }

    // public void addDockerConfig(String name, DockerConfiguration project) {
    //     dockerConfigurations.put(name, tmp)
    // }
    public HashMap<String, ProjectConfiguration> getProjectsConfigs(){
        return this.projects
    }
    // public HashMap<String, DockerConfiguration> getDockerConfigs(){
    //     return this.dockerConfigurations
    // }
}