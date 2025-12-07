package agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class ReceiverAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("ReceiverAgent стартовал " + getAID().getName() );

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg != null) {
                    System.out.println("Receiver получил: " + msg.getContent()
                            + " от " + msg.getSender().getLocalName());
                } else {
                    block();
                }
            }
        });
    }
}
