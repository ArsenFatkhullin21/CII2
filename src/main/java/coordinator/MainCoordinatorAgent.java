package coordinator;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.AgentContainer;
import product.Product;
import worker.Worker;

import java.util.List;

public class MainCoordinatorAgent extends Agent {

    private List<Product> products;
    private List<Worker> workers;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        products = (List<Product>) args[0];
        workers  = (List<Worker>) args[1];

        System.out.println("Coordinator стартовал: " + getAID().getName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    if (msg.getPerformative() == ACLMessage.INFORM &&
                            "DONE:p1".equals(msg.getContent())) {

                        System.out.println("Coordinator: получил DONE от p1, запускаю p2");

                        Product p2Config = products.stream()
                                .filter(p -> "p2".equals(p.getId()))
                                .findFirst()
                                .orElse(null);

                        if (p2Config != null) {
                            try {
                                AgentContainer container = getContainerController();
                                AgentController p2 = container.createNewAgent(
                                        "p2",
                                        "product.ProductAgent",
                                        new Object[]{p2Config, workers}
                                );
                                p2.start();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    block();
                }
            }
        });
    }
}
