package space.joserod.projects;
public class Project {
    String name;
    String version;
    ArrayList<Step> steps;

    public Project() {
        steps = new ArrayList<>();
    }
}