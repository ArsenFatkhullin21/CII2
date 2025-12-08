package machine;

import java.util.Arrays;

public class Machine {

    private String id;
    private String type;          // "Столяр", "Маляр" и т.п.
    private int[][] weekSchedule;

    public Machine() {
    }

    public Machine(int[][] weekSchedule, String type, String id) {
        this.weekSchedule = weekSchedule;
        this.type = type;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int[][] getWeekSchedule() {
        return weekSchedule;
    }

    public void setWeekSchedule(int[][] weekSchedule) {
        this.weekSchedule = weekSchedule;
    }

    @Override
    public String toString() {
        return "Machine{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", weekSchedule=" + Arrays.toString(weekSchedule) +
                '}';
    }
}
