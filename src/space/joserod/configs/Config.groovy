package space.joserod.configs;

import java.util.ArrayList;
import space.joserod.projects.*

public class Config {
    private HashMap<String, ProjectConfiguration> projects;
    private static Config config = new Config();

    private Config() {
        projects = new HashMap<>()
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

    public HashMap<String, ProjectConfiguration> getProjects(){
        return this.projects
    }
}