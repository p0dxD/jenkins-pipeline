package space.joserod.configs

public class ProjectConfiguration {
    private String name
    private String version
    private String path
    private LinkedHashMap values

    public ProjectConfiguration(final LinkedHashMap values) {
        this.name = values.name
        this.version = values.version
        this.path = values.path
        this.values = values
    }

    
}