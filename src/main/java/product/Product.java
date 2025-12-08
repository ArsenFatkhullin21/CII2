package product;

import java.util.List;
import java.util.Queue;

public class Product {
    private String id;
    private String name;
    private Queue<String> requiredSkills;
    private boolean busy;

    public Product() {
    }

    public Product(String id, String name, Queue<String> requiredSkills, boolean busy) {
        this.id = id;
        this.name = name;
        this.requiredSkills = requiredSkills;
        this.busy = busy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Queue<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(Queue<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", requiredSkills=" + requiredSkills +
                ", busy=" + busy +
                '}';
    }
}
