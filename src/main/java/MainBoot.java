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

        Runtime rt = Runtime.instance();
        AgentContainer main = rt.createMainContainer(new ProfileImpl());

// 1) запускаем всех работников
        for (Worker w : workers) {
            main.createNewAgent(
                    w.getId(),                     // уникальное имя агента
                    "worker.WorkerAgent",
                    new Object[]{w}
            ).start();
        }

// 2) координатор получает весь список изделий
        main.createNewAgent(
                "coordinator",
                "coordinator.MainCoordinatorAgent",
                new Object[]{products, workers}
        ).start();

// 3) запускаем только первое изделие из списка
        Product first = products.get(0);
        main.createNewAgent(
                first.getId(),
                "product.ProductAgent",
                new Object[]{first, workers}
        ).start();
    }
}
