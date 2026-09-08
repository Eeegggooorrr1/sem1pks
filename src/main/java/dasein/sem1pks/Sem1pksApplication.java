package dasein.sem1pks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Sem1pksApplication {

    public static void main(String[] args) {
        SpringApplication.run(Sem1pksApplication.class, args);
    }

}
