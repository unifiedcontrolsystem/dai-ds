package com.intel.networking.source.kafka;

import com.intel.logging.Logger;
import com.intel.networking.NetworkException;
import com.intel.networking.source.NetworkDataSource;
import com.intel.networking.source.NetworkDataSourceFactory;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class NetworkDataSourceKafka implements NetworkDataSource {

    /**
     * Used by the {@link NetworkDataSourceFactory} to create an instance of the Kafka provider.
     *
     * @param args A Map<String,String> where the following values are recognized:
     *
     * bootstrap.servers   - (req)     To establish an initial connection to the Kafka cluster.
     * schema.registry.url - (req)     Registered schema URL the provider is expecting message to send to.
     * key.deserializer    - (def: StringDeserializer)  To serialize key messages of any Avro/json type and send to Kafka.
     * value.deserializer  - (def: KafkaAvroSerializer) To serialize value messages of any Avro/json type and send to Kafka.

     */
    public NetworkDataSourceKafka(Logger logger, Map<String, String> args) {
        assert logger != null;
        assert args != null;
        logger_ = logger;
        args_ = args;
    }

    /**
     * Initialize the implementation.
     */
    @Override
    public void initialize() {
        for(String requiredKafkaProperty : requiredKafkaProperties) {
            if(!args_.containsKey(requiredKafkaProperty))
                throw new NetworkException("Missing required argument in kafkaSource configuration: '" + requiredKafkaProperty + "'");
            if(args_.containsValue(null) || args_.containsValue(""))
                throw new NetworkException("Given argument value cannot be null or empty. argument: '" + requiredKafkaProperty + "'");
        }

        kafkaProperties.setProperty(KAFKA_BOOTSTRAP_SERVER, args_.get(requiredKafkaProperties[0]));
        kafkaProperties.setProperty(KAFKA_SCHEMA_REG_URL, args_.get(requiredKafkaProperties[1]));
        kafkaProperties.setProperty(KAFKA_KEY_SERIALIZER, StringSerializer.class.getName());
        kafkaProperties.setProperty(KAFKA_VALUE_SERIALIZER, KafkaAvroSerializer.class.getName());
        //optional properties
        kafkaProperties.put(KAFKA_ACK, args_.getOrDefault(KAFKA_ACK, "all"));
        kafkaProperties.put(KAFKA_RETRY, args_.getOrDefault(KAFKA_RETRY, "10"));
    }

    /**
     * Sets the logger for the network source provider.
     *
     * @param logger Sets the {@link Logger} API instance into the provider so that it can also log errors/info to the
     *               owning process.
     */
    @Override
    public void setLogger(Logger logger) {
        if(logger != null)
            logger_ = logger;
    }

    /**
     * Get the factory name of the implemented provider.
     *
     * @return The name of the provider to be created by the {@link NetworkDataSourceFactory}.
     */
    @Override
    public String getProviderName() {
        return "kafka";
    }

    @Override
    public void connect(String info) {
    }

    void connect() {
        kafkaProducer_ = new KafkaProducer<>(kafkaProperties);
    }

    /**
     * Sends a message on a particular subject to the network.
     *
     * @param subject The subject to send the message for.
     * @param message The actual message to send.
     * @return True if the message was delivery, false otherwise.
     */
    @Override
    public boolean sendMessage(String subject, String message) {
        try {
            kafkaProducer_.send(new ProducerRecord<>(subject, message));
            kafkaProducer_.flush();
        } catch (Exception e) {
            logger_.error(e.getMessage());
            throw new NetworkException(e.getMessage());
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        if(kafkaProducer_ != null) {
            try {
                kafkaProducer_.close();
            } catch(Exception e) { logger_.exception(e); }
            kafkaProducer_ = null;
        }
    }

    private Logger logger_;
    private Producer<String, String> kafkaProducer_;

    private final Map<String,String> args_;
    private final Properties kafkaProperties = new Properties();

    private static final String KAFKA_KEY_SERIALIZER = "key.serializer";
    private static final String KAFKA_VALUE_SERIALIZER = "value.serializer";
    private static final String KAFKA_BOOTSTRAP_SERVER = "bootstrap.servers";
    private static final String KAFKA_SCHEMA_REG_URL = "schema.registry.url";
    private static final String KAFKA_ACK = "acks";
    private static final String KAFKA_RETRY = "retries";

    private static final String[] requiredKafkaProperties = {"bootstrap_servers", "schema_registry_url"};
}
