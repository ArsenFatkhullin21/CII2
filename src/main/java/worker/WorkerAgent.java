package worker;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import utils.SlotInfo;

public class WorkerAgent extends Agent {

    private Worker config;      // DTO из JSON
    private int[][] weekSchedule;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        config = (Worker) args[0];
        weekSchedule = config.getWeekSchedule();

        System.out.println("Worker " + config.getId() + " стартовал: " + getAID().getName());

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    switch (msg.getPerformative()) {
                        case ACLMessage.REQUEST:
                            System.out.println("Worker " + config.getId() +
                                    " получил запрос от " + msg.getSender().getLocalName()
                                    + ": " + msg.getContent());
                            logic(msg);
                            break;

                        case ACLMessage.ACCEPT_PROPOSAL:
                            System.out.println("Worker " + config.getId()
                                    + " ПРИНЯТ: " + msg.getContent());

                            SlotInfo info = splitter(msg); // day, slot, productId
                            weekSchedule[info.getDay()][info.getSlot()] = 1;  // теперь слот ЗАНЯТ
                            break;

                        case ACLMessage.REJECT_PROPOSAL:
                            System.out.println("Worker " + config.getId()
                                    + " ОТКЛОНЁН: " + msg.getContent());

                            break;
                    }
                } else {
                    block();
                }
            }

        });

    }

    private void logic(ACLMessage msg) {

        String[] parts = msg.getContent().split(":");
        int minDay  = Integer.parseInt(parts[0]);   // lastDay изделия
        int minSlot = Integer.parseInt(parts[1]);   // lastSlot изделия

        int day = -1;
        int slot = -1;
        boolean hasFreeSlot = false;

        // 1. Ищем первый ПОДХОДЯЩИЙ свободный слот
        for (int d = 0; d < weekSchedule.length; d++) {
            for (int s = 0; s < weekSchedule[d].length; s++) {
                if (weekSchedule[d][s] == 0) {
                    boolean isAfter =
                            d > minDay || (d == minDay && s > minSlot);
                    if (isAfter) {
                        hasFreeSlot = true;
                        day = d;
                        slot = s;
                        break;
                    }
                }
            }
            if (hasFreeSlot) break;
        }

        ACLMessage reply = msg.createReply();
        if (hasFreeSlot) {
            reply.setPerformative(ACLMessage.PROPOSE);
            reply.setContent(day + ":" + slot + ":" + config.getId());

            // ЛОГ: что предлагает этот работник
            System.out.println("Worker " + config.getId()
                    + " предлагает слот день=" + day + " слот=" + slot);
        } else {
            reply.setPerformative(ACLMessage.REFUSE);
            reply.setContent("NO_SLOT");

            System.out.println("Worker " + config.getId()
                    + " не нашёл подходящего свободного слота");
        }

        send(reply);
    }



    public SlotInfo splitter(ACLMessage msg) {
        String content = msg.getContent();      // "0:1:w1"
        String[] contentArray = content.split(":");

        int day = Integer.parseInt(contentArray[0]);
        int slot = Integer.parseInt(contentArray[1]);
        String workerName = contentArray[2];

        return new SlotInfo(day, slot, workerName);
    }



}
