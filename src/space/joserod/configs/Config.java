package space.joserod.configs;

import java.util.ArrayList;

public class Config {
    String name;
    String version;
    String configuration;
    ArrayList<Step> steps;

    public Config(String configuration) {
        this.configuration = configuration;
        steps = new ArrayList<>();
    }

    public void init() {
        
    }
}