package utils;

public class SlotInfo {
    private int day;
    private int slot;
    private String workerName;



    public SlotInfo(int day, int slot, String workerName) {
        this.day = day;
        this.slot = slot;
        this.workerName = workerName;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }
}
