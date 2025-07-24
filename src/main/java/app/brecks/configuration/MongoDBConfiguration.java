package app.brecks.configuration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.jsonwebtoken.lang.Collections;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.bson.codecs.pojo.Conventions.ANNOTATION_CONVENTION;

@Configuration
public class MongoDBConfiguration {
    private static final Logger logger = LogManager.getLogger();
    private final Environment environment;
    private MongoClient client;

    @Autowired
    public MongoDBConfiguration(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    private void setUpClient() {
        String USERNAME = this.environment.getProperty("additional-datasources.mongodb.username");
        String PASSWORD = this.environment.getProperty("additional-datasources.mongodb.password");
        String DB_HOST = this.environment.getProperty("additional-datasources.mongodb.host");
        String DB_PROTOCOL = this.environment.getProperty("additional-datasources.mongodb.protocol");
        String SOCKET_TIMEOUT_MS = this.environment.getProperty("additional-datasources.mongodb.socketTimeoutMS");
        String WAIT_QUEUE_TIMEOUT_MS = this.environment.getProperty("additional-datasources.mongodb.waitQueueTimeoutMS");
        String W_TIMEOUT_MS = this.environment.getProperty("additional-datasources.mongodb.wtimeoutMS");
        String APP_NAME = this.environment.getProperty("additional-datasources.mongodb.appName");
        String DB_CONNECTION_URL = String.format("%s://%s:%s@%s/?&tls=true&socketTimeoutMS=%s&waitQueueTimeoutMS=%s&wtimeoutMS=%s&retryWrites=true&w=majority&appName=%s", DB_PROTOCOL, USERNAME, PASSWORD, DB_HOST, SOCKET_TIMEOUT_MS, WAIT_QUEUE_TIMEOUT_MS, W_TIMEOUT_MS, APP_NAME);

        logger.info("Connecting to MongoDB using URL: {}", DB_CONNECTION_URL);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyToSslSettings(builder -> builder.enabled(true))
                .applyConnectionString(new ConnectionString(DB_CONNECTION_URL))
                .build();

        this.client = MongoClients.create(settings);
    }

    @Bean
    public MongoDatabase getDatabase() {
        CodecProvider codecProvider = PojoCodecProvider.builder().automatic(true).conventions(Collections.of(ANNOTATION_CONVENTION)).build();
        CodecRegistry codecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(codecProvider));
        return this.client
                .getDatabase(this.environment.getRequiredProperty("additional-datasources.mongodb.database"))
                .withCodecRegistry(codecRegistry);
    }

    @Bean
    public MongoClient getClient() {
        return this.client;
    }
}
