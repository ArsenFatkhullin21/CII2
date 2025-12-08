package machine;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import utils.SlotInfo;

import java.util.ArrayList;
import java.util.List;

public class MachineAgent extends Agent {

    private Machine config;
    private int[][] weekSchedule;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        config = (Machine) args[0];
        weekSchedule = config.getWeekSchedule();

        System.out.println("Machine " + config.getId() + " стартовал: " + getAID().getName());

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    switch (msg.getPerformative()) {
                        case ACLMessage.REQUEST:
                            logic(msg);
                            break;
                        case ACLMessage.ACCEPT_PROPOSAL:
                            SlotInfo info = SlotInfo.splitter(msg);
                            weekSchedule[info.getDay()][info.getSlot()] = 1;
                            System.out.println("Machine " + config.getId()
                                    + " забронировал " + info.getDay() + ":" + info.getSlot());
                            break;
                        case ACLMessage.REJECT_PROPOSAL:
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
        int day  = Integer.parseInt(parts[0]);   // слот работника
        int slot = Integer.parseInt(parts[1]);

        boolean hasFree = false;

        // проверяем ТОЛЬКО этот слот
        if (day >= 0 && day < weekSchedule.length &&
                slot >= 0 && slot < weekSchedule[day].length &&
                weekSchedule[day][slot] == 0) {
            hasFree = true;
        }

        ACLMessage reply = msg.createReply();
        if (hasFree) {
            reply.setPerformative(ACLMessage.PROPOSE);
            reply.setContent(day + ":" + slot + ":" + config.getId());
            System.out.println("Machine " + config.getId()
                    + " предлагает слот день=" + day + " слот=" + slot);
        } else {
            reply.setPerformative(ACLMessage.REFUSE);
            reply.setContent("NO_SLOT");
            System.out.println("Machine " + config.getId()
                    + " не имеет свободного слота " + day + ":" + slot);
        }
        send(reply);
    }




}

