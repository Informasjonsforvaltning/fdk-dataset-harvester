package no.dcat.harvester.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.dcat.datastore.AdminDataStore;
import no.dcat.datastore.Fuseki;
import no.dcat.harvester.crawler.Crawler;
import no.dcat.harvester.crawler.CrawlerJobFactory;
import no.dcat.harvester.settings.FusekiSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RabbitMQListener {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQListener.class);
    private static List<String> ALLOWED_FIELDS = Collections.singletonList("publisherId");
    private AdminDataStore adminDataStore;
    @Autowired
    private FusekiSettings fusekiSettings;
    @Autowired
    private Crawler crawler;
    @Autowired
    private CrawlerJobFactory crawlerJobFactory;
    private ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initialize() {
        adminDataStore = new AdminDataStore(new Fuseki(fusekiSettings.getAdminServiceUri()));
    }

    private Map<String, String> whitelistFields(Map<String, String> params) {
        return params.entrySet()
                .stream()
                .filter(x -> ALLOWED_FIELDS.contains(x.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // Will be used when integrated with fdk-harvest-admin
    private MultiValueMap<String, String> createQueryParams(Map<String, String> fields) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        whitelistFields(fields).forEach(params::add);
        return params;
    }

    private void harvest(Map<String, String> params) {
        logger.info("Harvesting Dataset Publisher");

        // temp solution before migrating to fdk-harvest-admin
        adminDataStore.getDcatSources().stream()
                .filter(dcatSource -> params.get("publisherId").equalsIgnoreCase(dcatSource.getOrgnumber()))
                .forEach(dcatSource -> {
                    try {
                        crawler.execute(crawlerJobFactory.createCrawlerJob(dcatSource)).get();
                    } catch (Exception e) {
                        logger.error("EXECUTION ERROR ", e);
                    }
                });
    }

    @RabbitListener(queues = "#{queue.name}")
    public void receiveDatasetMessage(@Payload JsonNode body, Message message) {
        String[] routingKey = message.getMessageProperties().getReceivedRoutingKey().split("\\.");
        logger.info("Received message from key: {}.{}", routingKey[0], routingKey[1]);

        // convert from map to multivaluemap for UriComponentBuilder
        Map<String, String> fields = objectMapper.convertValue(
                body,
                new TypeReference<Map<String, String>>() {
                });

        harvest(fields);
    }
}
