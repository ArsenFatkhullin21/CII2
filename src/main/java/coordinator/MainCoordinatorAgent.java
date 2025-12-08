package coordinator;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.AgentContainer;
import machine.Machine;
import product.Product;
import worker.Worker;

import java.util.List;
public class MainCoordinatorAgent extends Agent {
    private List<Product> products;
    private List<Worker> workers;
    private List<Machine> machines;
    private int currentIndex = 0;
    private boolean startedFirst = false;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        products = (List<Product>) args[0];
        workers  = (List<Worker>) args[1];
        machines = (List<Machine>) args[2];

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null &&
                        msg.getPerformative() == ACLMessage.INFORM &&
                        msg.getContent().startsWith("DONE:")) {

                    String doneId = msg.getContent().substring("DONE:".length());
                    System.out.println("Coordinator: завершено изделие " + doneId);

                    currentIndex++;
                    if (currentIndex < products.size()) {
                        startProduct(products.get(currentIndex));
                    } else {
                        System.out.println("Coordinator: все изделия обработаны");
                    }
                } else {
                    block();
                }
            }
        });
    }

    private void startProduct(Product p) {
        try {
            AgentController ac = getContainerController().createNewAgent(
                    p.getId(),                    // имя = id изделия
                    "product.ProductAgent",
                    new Object[]{p, workers, machines}
            );
            ac.start();
            System.out.println("Coordinator: запущено изделие " + p.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
