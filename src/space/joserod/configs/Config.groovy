package space.joserod.configs

import space.joserod.configs.ProjectConfiguration

public class Config {
    private final HashMap<String, ProjectConfiguration> projects
    private static Config config = new Config()

    private Config() {
        projects = new HashMap<>()
    }

    public static Config getInstance() {
        return config
    }

    public void addProject(String name, LinkedHashMap values) {
        projects.put(name, new ProjectConfiguration(values))
    }

    public void addProject(String name, ProjectConfiguration project) {
        projects.put(name, project)
    }

    public HashMap<String, ProjectConfiguration> getProjectsConfigs() {
        return this.projects
    }
}
