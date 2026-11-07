package org.lawaterwatch.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class WaterApiApplication {

    //application.properties builds the database path from this, so data/ lands in water-api/ whatever the working dir
    static final String HOME_PROPERTY = "app.home";

    public static void main(String[] args) throws IOException {
        Path home = appHome();
        System.setProperty(HOME_PROPERTY, home.toString());
        Files.createDirectories(home.resolve("data"));
        SpringApplication.run(WaterApiApplication.class, args);
    }

    //Compiled classes live in water-api/target/classes, so two levels up is water-api/
    //Falls back to the working directory when the classes aren't on disk (e.g. inside a packaged jar)
    static Path appHome() {
        try {
            URL location = WaterApiApplication.class.getProtectionDomain().getCodeSource().getLocation();
            if ("file".equals(location.getProtocol())) {
                Path target = Path.of(location.toURI()).getParent();
                if (target != null && target.getFileName().toString().equals("target") && target.getParent() != null) {
                    return target.getParent();
                }
            }
        } catch (URISyntaxException | SecurityException e) {
            //Fall through to the working directory
        }
        return Path.of("").toAbsolutePath();
    }

}
