package space.joserod.configs

import java.util.ArrayList
import space.joserod.configs.ProjectConfiguration
import space.joserod.configs.DockerConfiguration

/**
 * A The configurations loaded from the project.
 */
@Singleton
public class Config {

    /** The loaded project configurations **/
    private final HashMap<String, ProjectConfiguration> projectsConfigs = new HashMap<>();

    public void addProject(String name, LinkedHashMap values) {
        ProjectConfiguration tmp = new ProjectConfiguration(values)
        projectsConfigs.put(name, tmp)
    }

    public void addProject(String name, ProjectConfiguration project) {
        projectsConfigs.put(name, tmp)
    }

}