package space.joserod.configs

public class ProjectConfiguration {
    private final String name
    private final String version
    private final String path
    private final LinkedHashMap values

    public ProjectConfiguration(final LinkedHashMap values) {
        this.name = values.name
        this.version = values.version
        this.path = values.path
        this.values = values
    }

    
}