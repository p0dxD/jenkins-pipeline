package space.joserod.configs;

import java.util.ArrayList;
import space.joserod.project.Project

public class Config {
    HashMap<String, Project> projects;

    public Config() {
        this.configuration = configuration
        projects = new HashMap<>()
    }

    public void addProject(String name, String values) {
        Project tmp = new Project(values)
        project.put(name, tmp)
    }

    public void addProject(String name, Project project) {
        project.put(name, tmp)
    }

    public HashMap<String, Project> getProjects(){
        return this.projects
    }
}