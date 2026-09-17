package dasein.sem1pks.bootstrap;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BootstrapProperties.class)
public class BootstrapConfiguration {
    @Bean
    @ConditionalOnProperty(name="app.bootstrap.enabled", havingValue="true")
    ApplicationRunner bootstrapRunner(BootstrapService service) {
        return args -> service.initialize();
    }
}
