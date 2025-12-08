package utils;

import jade.lang.acl.ACLMessage;

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

    public static SlotInfo splitter(ACLMessage msg) {
        String content = msg.getContent();      // "0:1:w1"
        String[] contentArray = content.split(":");

        int day = Integer.parseInt(contentArray[0]);
        int slot = Integer.parseInt(contentArray[1]);
        String workerName = contentArray[2];

        return new SlotInfo(day, slot, workerName);
    }
}
