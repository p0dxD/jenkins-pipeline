package space.joserod.configs;

import java.util.ArrayList;
import space.joserod.projects.*

public class Config {
    HashMap<String, ProjectConfiguration> projects;

    public Config() {
        this.configuration = configuration
        projects = new HashMap<>()
    }

    public void addProject(String name, String values) {
        Project tmp = new Project(values)
        project.put(name, tmp)
    }

    public void addProject(String name, ProjectConfiguration project) {
        project.put(name, tmp)
    }

    public HashMap<String, ProjectConfiguration> getProjects(){
        return this.projects
    }
}