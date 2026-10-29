package org.lawaterwatch.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

//Own database file and no startup load, so the test never calls the live APIs or touches data/water.db
@SpringBootTest(properties = {
        "app.data.load-on-startup=false",
        "spring.datasource.url=jdbc:sqlite:target/test-water.db"
})
class WaterApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
