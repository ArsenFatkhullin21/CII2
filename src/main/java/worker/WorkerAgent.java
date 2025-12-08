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

        int day = -1;
        int slot = -1;
        boolean hasFreeSlot = false;

        // 1. Ищем первый свободный слот по всей неделе
        for (int d = 0; d < weekSchedule.length; d++) {                // дни
            for (int s = 0; s < weekSchedule[d].length; s++) {         // слоты в дне
                if (weekSchedule[d][s] == 0) {                         // 0 = свободен
                    hasFreeSlot = true;
                    day = d;
                    slot = s;
                    break;
                }
            }
            if (hasFreeSlot) {
                break; // выходим и из внешнего цикла, слот найден
            }
        }

        ACLMessage reply = msg.createReply();
        if (hasFreeSlot) {
            reply.setPerformative(ACLMessage.PROPOSE);
            reply.setContent(day + ":" + slot + ":" + config.getId()); // например, "0:1:w3"

        } else {
            reply.setPerformative(ACLMessage.REFUSE);
            reply.setContent("NO_SLOT");
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
