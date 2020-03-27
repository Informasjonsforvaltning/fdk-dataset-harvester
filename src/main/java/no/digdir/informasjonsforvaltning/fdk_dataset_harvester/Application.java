package no.digdir.informasjonsforvaltning.fdk_dataset_harvester;

import no.digdir.informasjonsforvaltning.fdk_dataset_harvester.configuration.FusekiProperties;
import org.apache.jena.riot.RIOT;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ FusekiProperties.class })
public class Application {

    public static void main(String[] args) {
        RIOT.init();
        SpringApplication.run(Application.class, args);
    }

}