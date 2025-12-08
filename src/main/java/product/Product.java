package product;

import java.util.List;
import java.util.Queue;

public class Product {
    private String id;
    private String name;
    private Queue<String> requiredSkills;
    private boolean busy;

    private int lastDay = 0;
    private int lastSlot = -1;


    public Product() {
    }

    public Product(String id, String name, Queue<String> requiredSkills, boolean busy, int lastDay, int lastSlot) {
        this.id = id;
        this.name = name;
        this.requiredSkills = requiredSkills;
        this.busy = busy;
        this.lastDay = lastDay;
        this.lastSlot = lastSlot;
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

    public int getLastDay() {

        return lastDay;
    }

    public void setLastDay(int lastDay) {
        this.lastDay = lastDay;
    }

    public int getLastSlot() {
        return lastSlot;
    }

    public void setLastSlot(int lastSlot) {
        this.lastSlot = lastSlot;
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

    public void setLast(int day, int slot) {
        this.lastDay = day;
        this.lastSlot = slot;
    }
}
