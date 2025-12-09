package agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import utils.ExcelLogger;

public class ReceiverAgent extends Agent {

    @Override
    protected void setup() {
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String text = getLocalName() + " получил: " + msg.getContent();
                    System.out.println(text);
                    ExcelLogger.log(getLocalName(), text);
                } else {
                    block();
                }
            }
        });
    }
}
