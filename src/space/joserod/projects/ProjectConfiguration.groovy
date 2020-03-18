package space.joserod.projects;

public class ProjectConfiguration {
    String name;
    String version;
    String values;

    public ProjectConfiguration(LinkedHashMap values) {
        this.name = values.name
        this.version = values.version
        this.values = values
    }
}