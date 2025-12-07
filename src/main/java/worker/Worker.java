package worker;

import java.util.Arrays;
import java.util.List;

public class Worker {
    private String id;
    private String name;
    private List<String> skills;
    private int [][] weekSchedule;


    public boolean isFree(int day,int slot){
        if (weekSchedule[day][slot] == 0){
            return true;
        } else {
            return false;
        }
    }

    public void book(int day,int slot){
        weekSchedule[day][slot] = 1;
    }

    public Worker() {
    }

    public Worker(String id, String name, List<String> skills, int[][] weekSchedule) {
        this.id = id;
        this.name = name;
        this.skills = skills;
        this.weekSchedule = weekSchedule;
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

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public int[][] getWeekSchedule() {
        return weekSchedule;
    }

    public void setWeekSchedule(int[][] weekSchedule) {
        this.weekSchedule = weekSchedule;
    }

    @Override
    public String toString() {
        return "Worker{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", skills=" + skills +
                ", weekSchedule=" + Arrays.toString(weekSchedule) +
                '}';
    }
}
