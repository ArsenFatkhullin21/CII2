package product;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import worker.Worker;

import java.util.ArrayList;
import java.util.List;



public class ProductAgent extends Agent {

    private Product product;
    private List<Worker> allWorkers;
    private List<AID> proposers = new ArrayList<>();


    @Override
    protected void setup() {
        Object[] args = getArguments();
        // 0 — конфиг изделия, 1 — список всех работников (можно передать по‑другому)
        product = (Product) args[0];
        allWorkers = (List<Worker>) args[1];

        System.out.println("Product " + product.getId() + " стартовал: " + getAID().getName());

        addBehaviour(new RequestWorkersBehaviour() );
    }

    private class RequestWorkersBehaviour extends Behaviour {
        private boolean finishedSelection = false;
        private boolean sent = false;
        private int replies = 0;
        private int expectedReplies = 0;

        private int bestDay = Integer.MAX_VALUE;
        private int bestSlot = Integer.MAX_VALUE;
        private AID bestWorker = null;

        private final List<AID> proposers = new ArrayList<>();

        @Override
        public void onStart() {
            // просто смотрим, не вынимая
            String skill = product.getRequiredSkills().peek();
            if (skill == null) {
                finishedSelection = true;
                return;
            }

            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setContent("Нужен " + skill + " для изделия " + product.getId());

            for (Worker w : allWorkers) {
                if (w.getSkills().contains(skill)) {
                    msg.addReceiver(new AID(w.getId(), AID.ISLOCALNAME));
                    expectedReplies++;
                }
            }

            if (expectedReplies > 0) {
                send(msg);
                System.out.println("Product " + product.getId()
                        + " отправил запрос работникам с навыком " + skill);
            }
            sent = true;
        }


        @Override
        public void action() {
            if (finishedSelection) {
                block();
                return;
            }

            ACLMessage reply = receive();
            if (reply != null) {
                replies++;

                if (reply.getPerformative() == ACLMessage.PROPOSE) {
                    String[] parts = reply.getContent().split(":");
                    int day = Integer.parseInt(parts[0]);
                    int slot = Integer.parseInt(parts[1]);

                    System.out.println("Получено предложение от "
                            + reply.getSender().getLocalName()
                            + " день=" + day + " слот=" + slot);

                    proposers.add(reply.getSender());

                    if (day < bestDay || (day == bestDay && slot < bestSlot)) {
                        bestDay = day;
                        bestSlot = slot;
                        bestWorker = reply.getSender();
                    }

                } else if (reply.getPerformative() == ACLMessage.REFUSE) {
                    System.out.println("Агент " + reply.getSender().getLocalName()
                            + " отказался: " + reply.getContent());
                }

                if (sent && replies >= expectedReplies) {
                    try {
                        sendDecisions();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    finishedSelection = true;
                }
            } else {
                block();
            }
        }

        private void sendDecisions() throws InterruptedException {
            if (bestWorker == null) {
                System.out.println("Подходящих предложений нет");
                return;
            }

            // 1) ACCEPT лучшему и REJECT остальным
            senfAcceptReject();
            Thread.sleep(500);


            // 2) помечаем текущий навык как выполненный
            String doneSkill = product.getRequiredSkills().poll();
            System.out.println("Для изделия " + product.getId()
                    + " выполнен этап: " + doneSkill);

            // 3) есть ещё этапы?
            if (!product.getRequiredSkills().isEmpty()) {
                // запускаем следующий поиск (следующий skill в очереди)
                myAgent.addBehaviour(new RequestWorkersBehaviour());
            } else {
                System.out.println("Изделие " + product.getId() + " полностью спланировано");

                // 4) только теперь шлём DONE координатору
                ACLMessage done = new ACLMessage(ACLMessage.INFORM);
                done.addReceiver(new AID("coordinator", AID.ISLOCALNAME));
                done.setContent("DONE:" + product.getId());
                send(done);
            }
        }

        private void senfAcceptReject(){
            // ACCEPT лучшему
            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accept.addReceiver(bestWorker);
            accept.setContent(bestDay + ":" + bestSlot + ":" + product.getId());


            send(accept);


            // REJECT остальным
            for (AID proposer : proposers) {
                if (!proposer.equals(bestWorker)) {
                    ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    reject.addReceiver(proposer);
                    reject.setContent("Ваше предложение отклонено для изделия " + product.getId());
                    send(reject);

                }

            }

            System.out.println("Отправлен ACCEPT " + bestWorker.getLocalName()
                    + " и REJECT остальным");
        }

        @Override
        public boolean done() {
            return finishedSelection;
        }
    }


}
