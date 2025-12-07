import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import product.Product;
import product.ProductsFile;
import utils.JsonConfigLoader;
import worker.Worker;
import worker.WorkersFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MainBoot {

    static Path producstPath = Path.of("/Users/arsenfatkhulllin/Desktop/learn/CII2/files/products.json");
    static Path workersPath  = Path.of("/Users/arsenfatkhulllin/Desktop/learn/CII2/files/workers.json");

    public static void main(String[] args) throws Exception {
        // 1. Читаем JSON
        ProductsFile productsFile = JsonConfigLoader.load(producstPath, ProductsFile.class);
        WorkersFile  workersFile  = JsonConfigLoader.load(workersPath,  WorkersFile.class);

        List<Product> products = productsFile.getProducts();
        List<Worker>  workers  = workersFile.getWorkers();

        // 2. Поднимаем платформу программно (без Boot.main)
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true"); // чтобы был RMA
        AgentContainer mainContainer = rt.createMainContainer(profile);

        // 3. Создаём работников
        for (Worker w : workers) {
            AgentController ac = mainContainer.createNewAgent(
                    w.getId(),                            // имя агента на платформе
                    "worker.WorkerAgent",                 // FQCN твоего WorkerAgent
                    new Object[]{w}                       // аргументы в setup()
            );
            ac.start();
        }

        // 4. Создаём изделия
        for (Product p : products) {
            AgentController ac = mainContainer.createNewAgent(
                    p.getId(),
                    "product.ProductAgent",               // FQCN твоего ProductAgent
                    new Object[]{p, workers}              // изделию можно передать список работников
            );
            ac.start();
        }
    }
}
