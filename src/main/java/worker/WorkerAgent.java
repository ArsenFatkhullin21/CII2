package worker;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

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
                    System.out.println("Worker " + config.getId() +
                            " получил запрос от " + msg.getSender().getLocalName()
                            + ": " + msg.getContent());

                    logic(msg);

                    switch (msg.getPerformative()) {
                        case ACLMessage.REQUEST:
                            logic(msg); // ищем слот и шлём PROPOSE/REFUSE
                            break;
                        case ACLMessage.ACCEPT_PROPOSAL:
                            System.out.println("Worker " + config.getId()
                                    + " ПРИНЯТ: " + msg.getContent());
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

        // 2. Формируем ОДИН ответ на сообщение
        ACLMessage reply = msg.createReply();
        if (hasFreeSlot) {
            reply.setPerformative(ACLMessage.PROPOSE);
            reply.setContent(day + ":" + slot);  // каждый агент шлёт свой day:slot
            weekSchedule[day][slot] = 1;         // бронируем этот слот
        } else {
            reply.setPerformative(ACLMessage.REFUSE);
            reply.setContent("NO_SLOT");
        }

        send(reply);
    }

}
