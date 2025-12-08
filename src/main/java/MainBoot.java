import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import product.Product;
import product.ProductsFile;
import machine.MachinesFile;
import machine.Machine;
import utils.JsonConfigLoader;
import worker.Worker;
import worker.WorkersFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MainBoot {

    static Path producstPath = Path.of("/Users/arsenfatkhulllin/Desktop/learn/CII2/files/products.json");
    static Path workersPath  = Path.of("/Users/arsenfatkhulllin/Desktop/learn/CII2/files/workers.json");
    static Path machinesPath = Path.of("/Users/arsenfatkhulllin/Desktop/learn/CII2/files/machines.json");

    public static void main(String[] args) throws Exception {
        ProductsFile productsFile = JsonConfigLoader.load(producstPath, ProductsFile.class);
        WorkersFile  workersFile  = JsonConfigLoader.load(workersPath,  WorkersFile.class);
        MachinesFile machinesFile = JsonConfigLoader.load(machinesPath, MachinesFile.class);

        List<Product> products = productsFile.getProducts();
        List<Worker>  workers  = workersFile.getWorkers();
        List<Machine> machines = machinesFile.getMachines();

        Runtime rt = Runtime.instance();
        AgentContainer main = rt.createMainContainer(new ProfileImpl());

        // 1) работники
        for (Worker w : workers) {
            main.createNewAgent(
                    w.getId(),
                    "worker.WorkerAgent",
                    new Object[]{w}
            ).start();
        }

        for(Machine m : machines) {
            main.createNewAgent(
                    m.getId(),
                    "machine.MachineAgent",
                    new Object[]{m}
            ).start();
        }

        // координатор
        main.createNewAgent(
                "coordinator",
                "coordinator.MainCoordinatorAgent",
                new Object[]{products, workers, machines}
        ).start();

// запустить первое изделие (остальные пойдут по DONE)
        Product first = products.get(0);
        main.createNewAgent(
                first.getId(),
                "product.ProductAgent",
                new Object[]{first, workers, machines}
        ).start();    }

}
