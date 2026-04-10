package logo.analysis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;

public class Logger {

    private static final Path LOG_FILE = Path.of("/tmp/logolsp-debug.log");

    public static void log(String message) {
        try {
            Files.writeString(
                LOG_FILE,
                "[" + LocalTime.now() + "] " + message + "\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            // ignore — logging should never crash the server
        }
    }

    public static void log(String component, String message) {
        log("[" + component + "] " + message);
    }
}
