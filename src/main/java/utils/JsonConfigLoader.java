package utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonConfigLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static <T> T load(Path path, Class<T> clazz) throws IOException {
        try (java.io.InputStream in = Files.newInputStream(path)) {
            return MAPPER.readValue(in, clazz);
        }
    }
}
