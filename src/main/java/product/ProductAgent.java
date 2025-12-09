package product;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import machine.Machine;
import utils.ExcelLogger;
import worker.Worker;

import java.util.ArrayList;
import java.util.List;



public class ProductAgent extends Agent {

    private Product product;
    private List<Worker> allWorkers;
    private List<Machine> allMachines;
    private List<AID> proposers = new ArrayList<>();
    private List<WorkerOffer> workerOffers = new ArrayList<>();
    private boolean doneSent = false;




    @Override
    protected void setup() {
        Object[] args = getArguments();
        product     = (Product) args[0];
        allWorkers  = (List<Worker>) args[1];
        allMachines = (List<Machine>) args[2];


        log("Product " + product.getId() + " стартовал: " + getAID().getName());
        addBehaviour(new RequestWorkersBehaviour());
    }


    private class RequestWorkersBehaviour extends Behaviour {

        private boolean finished = false;
        private boolean sent = false;
        private int replies = 0;
        private int expectedReplies = 0;

        private int bestDay = Integer.MAX_VALUE;
        private int bestSlot = Integer.MAX_VALUE;
        private AID bestWorker = null;

        @Override
        public void onStart() {
            workerOffers.clear();

            String skill = product.getRequiredSkills().peek();
            if (skill == null) {
                finished = true;
                return;
            }

            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setContent(product.getLastDay() + ":" + product.getLastSlot()
                    + ":" + skill + ":" + product.getId());

            for (Worker w : allWorkers) {
                if (w.getSkills().contains(skill)) {
                    msg.addReceiver(new AID(w.getId(), AID.ISLOCALNAME));
                    expectedReplies++;
                }
            }

            if (expectedReplies > 0) {
                send(msg);
                log("Product " + product.getId()
                        + " отправил запрос работникам с навыком " + skill);
            } else {
                log("Нет работников с навыком " + skill);
                finished = true;
            }
            sent = true;
        }

        @Override
        public void action() {
            if (finished) {
                block();
                return;
            }

            ACLMessage reply = receive();
            if (reply != null) {
                replies++;

                if (reply.getPerformative() == ACLMessage.PROPOSE) {
                    String[] parts = reply.getContent().split(":");
                    int day  = Integer.parseInt(parts[0]);
                    int slot = Integer.parseInt(parts[1]);

                    log("Получено предложение от "
                            + reply.getSender().getLocalName()
                            + " день=" + day + " слот=" + slot);

                    workerOffers.add(new WorkerOffer(reply.getSender(), day, slot));

                    // параллельно запоминаем лучший по времени (для первого кандидата)
                    if (day < bestDay || (day == bestDay && slot < bestSlot)) {
                        bestDay = day;
                        bestSlot = slot;
                        bestWorker = reply.getSender();
                    }

                } else if (reply.getPerformative() == ACLMessage.REFUSE) {
                    log("Агент " + reply.getSender().getLocalName()
                            + " отказался: " + reply.getContent());
                }

                if (sent && replies >= expectedReplies) {
                    // все ответы получены — переходим к поиску станка
                    startCheckMachines();
                    finished = true;
                }
            } else {
                block();
            }
        }

        private void startCheckMachines() {
            // сортируем предложения работников по времени
            workerOffers.sort((a, b) -> {
                if (a.day != b.day) return Integer.compare(a.day, b.day);
                return Integer.compare(a.slot, b.slot);
            });

            // запускаем поведение, которое по очереди проверяет каждого работника
            addBehaviour(new CheckWorkersAgainstMachinesBehaviour());
        }

        @Override
        public boolean done() {
            return finished;
        }
    }

    private void log(String message) {
        String msg = message;
        System.out.println(msg);
        ExcelLogger.log(getLocalName(), msg);
    }

    private class RequestMachinesBehaviour extends Behaviour {

        private final WorkerOffer worker;
        private final CheckWorkersAgainstMachinesBehaviour parent;

        private boolean sent = false;
        private boolean finished = false;
        private int replies = 0;
        private int expectedReplies = 0;

        private AID bestMachine = null;
        private String convId;

        private MessageTemplate mt;

        RequestMachinesBehaviour(WorkerOffer worker, CheckWorkersAgainstMachinesBehaviour parent) {
            this.worker = worker;
            this.parent = parent;
            this.convId = "MACH-" + product.getId() + "-" + System.currentTimeMillis();
            this.mt = MessageTemplate.MatchConversationId(convId);
        }

        @Override
        public void onStart() {
            String skill = product.getRequiredSkills().peek();

            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setConversationId(convId);                 // ← важное отличие
            msg.setContent(worker.day + ":" + worker.slot);

            for (Machine m : allMachines) {
                if (m.getType().equals(skill)) {
                    msg.addReceiver(new AID(m.getId(), AID.ISLOCALNAME));
                    expectedReplies++;
                }
            }

            if (expectedReplies > 0) {
                send(msg);
                log("Product " + product.getId()
                        + " проверяет станки для работника "
                        + worker.workerAID.getLocalName()
                        + " слот=" + worker.day + ":" + worker.slot);
            } else {
                finished = true;
                parent.onMachinesResult(worker, null, -1, -1, false);
            }
            sent = true;
        }

        @Override
        public void action() {
            if (finished) {
                block();
                return;
            }

            ACLMessage reply = myAgent.receive(mt);  // вместо лямбды

            if (reply != null) {
                replies++;

                if (reply.getPerformative() == ACLMessage.PROPOSE) {
                    bestMachine = reply.getSender();
                }

                if (sent && replies >= expectedReplies) {
                    boolean success = (bestMachine != null);
                    finished = true;
                    if (success) {
                        parent.onMachinesResult(worker, bestMachine,
                                worker.day, worker.slot, true);
                    } else {
                        parent.onMachinesResult(worker, null, -1, -1, false);
                    }
                }
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return finished;
        }
    }




    private static class WorkerOffer {
        AID workerAID;
        int day;
        int slot;

        WorkerOffer(AID aid, int day, int slot) {
            this.workerAID = aid;
            this.day = day;
            this.slot = slot;
        }
    }


    private class CheckWorkersAgainstMachinesBehaviour extends Behaviour {

        private int index = 0;
        private boolean finished = false;
        private boolean waitingForMachines = false;

        // результат
        private WorkerOffer chosenWorker = null;
        private AID chosenMachine = null;
        private int chosenDay;
        private int chosenSlot;

        @Override
        public void action() {
            if (finished) {
                block();
                return;
            }

            if (!waitingForMachines) {
                if (index >= workerOffers.size()) {
                    log("Для изделия " + product.getId()
                            + " не найдено пары Работник+Станок");
                    finished = true;
                    return;
                }

                WorkerOffer candidate = workerOffers.get(index);
                addBehaviour(new RequestMachinesBehaviour(candidate, this));
                waitingForMachines = true;   // важный флаг
            } else {
                // ждём, пока RequestMachinesBehaviour вызовет onMachinesResult
                block();
            }
        }

        // вызывается из RequestMachinesBehaviour при успехе/неуспехе
        void onMachinesResult(WorkerOffer worker, AID machine, int day, int slot, boolean success) {
            waitingForMachines = false;      // освободили слот

            if (finished) return;           // защита от повторного вызова

            if (success) {
                chosenWorker = worker;
                chosenMachine = machine;
                chosenDay = day;
                chosenSlot = slot;
                finalizePair();
                finished = true;
            } else {
                index++;                    // пробуем следующего работника
                restart();                  // вернуться в action() с новым index
            }
        }

        private void finalizePair() {
            log("Изделие " + product.getId()
                    + " выбрало Работника " + chosenWorker.workerAID.getLocalName()
                    + " и Станок " + chosenMachine.getLocalName()
                    + " слот=" + chosenDay + ":" + chosenSlot);

            // ACCEPT работнику
            ACLMessage accW = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accW.addReceiver(chosenWorker.workerAID);
            accW.setContent(chosenDay + ":" + chosenSlot + ":" + product.getId());
            send(accW);

            // REJECT всем остальным работникам, которые делали PROPOSE
            for (WorkerOffer wOff : workerOffers) {
                if (!wOff.workerAID.equals(chosenWorker.workerAID)) {
                    ACLMessage rej = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    rej.addReceiver(wOff.workerAID);
                    rej.setContent("REJECT:" + product.getId());
                    send(rej);
                }
            }

            // ACCEPT выбранному станку
            ACLMessage accM = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accM.addReceiver(chosenMachine);
            accM.setContent(chosenDay + ":" + chosenSlot + ":" + product.getId());
            send(accM);

            // обновляем «хвост» изделия
            product.setLast(chosenDay, chosenSlot);

            // этап выполнен
            String doneSkill = product.getRequiredSkills().poll();
            log("Для изделия " + product.getId()
                    + " выполнен этап: " + doneSkill);
            ExcelLogger.recordWorkerBooking(
                    chosenWorker.workerAID.getLocalName(),
                    chosenDay,
                    chosenSlot,
                    product.getId(),
                    chosenMachine.getLocalName(),
                    doneSkill
            );

            if (!product.getRequiredSkills().isEmpty()) {
                addBehaviour(new RequestWorkersBehaviour());
            } else {
                log("Изделие " + product.getId() + " полностью спланировано");
                if (!doneSent) {                    // защита от повторного DONE
                    doneSent = true;
                    ACLMessage done = new ACLMessage(ACLMessage.INFORM);
                    done.addReceiver(new AID("coordinator", AID.ISLOCALNAME));
                    done.setContent("DONE:" + product.getId());
                    send(done);
                }
            }
        }


        @Override
        public boolean done() {
            return finished;
        }
    }


}
