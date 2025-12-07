package agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class SenderAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("SenderAgent стартовал: " + getAID().getName());

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

        msg.addReceiver(new AID("receiver", AID.ISLOCALNAME));
        msg.setContent("Привет от Sender");

        send(msg);
        System.out.println("Sender отправил сообщение");
    }
}
