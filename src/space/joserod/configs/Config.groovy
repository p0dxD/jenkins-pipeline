package space.joserod.configs;

import java.util.ArrayList;
import space.joserod.projects.*

public class Config {
    private HashMap<String, ProjectConfiguration> projects;
    private static Config something = new Config();

    private Config() {
        projects = new HashMap<>()
    }

   //Get the only object available
   public static Config getInstance(){
      return something;
   }

    public void addProject(String name, String values) {
        ProjectConfiguration tmp = new ProjectConfiguration(values)
        project.put(name, tmp)
    }

    public void addProject(String name, ProjectConfiguration project) {
        project.put(name, tmp)
    }

    public HashMap<String, ProjectConfiguration> getProjects(){
        return this.projects
    }
}