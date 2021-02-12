package com.intel.networking.sink.kafka;

import com.intel.logging.Logger;
import com.intel.networking.NetworkException;
import com.intel.networking.sink.NetworkDataSinkDelegate;
import com.intel.networking.sink.NetworkDataSinkEx;
import com.intel.networking.sink.StreamLocationHandler;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Kafka client implementation of a {@link NetworkDataSinkEx} providers. Subject refers to the Kafka client consuming topics.
 */
public class NetworkDataSinkKafka implements NetworkDataSinkEx, Runnable {

    /**
     * Create a network sink object for Kafka client.
     * @param args The arguments to connect kafka bus. Supported argument keys are:
     *
     *   bootstrap.servers    - (req)     To establish an initial connection to the Kafka cluster.
     *   group.id             - (req)     Identifies the consumer group of a given consumer.     *
     *   schema.registry.url  - (req)     Registered schema URL the consumer is expecting message to conform to.
     *             
     *   auto.commit.enable   - (def: false)  Consumer's offset will be periodically committed in the background.
     *   auto.offset.reset    - (def: earliest)  Offset from which next new record will be fetched
     *                                       Used ONLY if consumer don't have a valid offset committed.(earliest/latest)
     *   specific.avro.reader - (def: true)  Consumer expects avro type messages.
     *   key.deserializer     - (def: StringDeserializer)  To deserialize received key messages of any Avro/json type from Kafka.
     *   value.deserializer   - (def: KafkaAvroSerializer) To deserialize received value messages of any Avro/json type from Kafka.
     */
    public NetworkDataSinkKafka(Logger logger, Map<String,String> args) {
        assert logger != null;
        assert args != null;
        logger_ = logger;
        args_ = args;
    }

    /**
     * used to initialize the implementation.
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
        kafkaProperties.put(KAFKA_GROUP_ID, args_.get(requiredKafkaProperties[1]));
        kafkaProperties.setProperty(KAFKA_SCHEMA_REG_URL, args_.get(requiredKafkaProperties[2]));

        kafkaProperties.put(KAFKA_AUTO_COMMIT, args_.getOrDefault(optionalKafkaProperties[0], "false"));
        kafkaProperties.put(KAFKA_AUTO_OFFSET, args_.getOrDefault(optionalKafkaProperties[1], "earliest"));
        kafkaProperties.setProperty(KAFKA_IS_AVRO_FORMAT, args_.getOrDefault(optionalKafkaProperties[2], "true"));
        kafkaProperties.setProperty(KAFKA_KEY_DESERIALIZER, StringDeserializer.class.getName());
        kafkaProperties.setProperty(KAFKA_VALUE_DESERIALIZER, KafkaAvroSerializer.class.getName());
        timeout_ = args_.getOrDefault(KAFKA_TIMEOUT, "60");
    }

    /**
     * Clear all previously set subjects. Has no effect if startListening was already called.
     */
    @Override
    public void clearSubjects() {
        subjects_.clear();
    }

    /**
     * Add a subject to this object. Must be called prior to startListening.
     *
     * @param subject The string that is the subject to add for listening on the network bus.
     */
    @Override
    public void setMonitoringSubject(String subject) {
        subjects_.add(subject);
    }

    /**
     * A collection of subjects to this object. Must be called prior to startListening.
     *
     * @param subjects The collection of subjects to add for listening on the kafka network bus.
     */
    @Override
    public void setMonitoringSubjects(Collection<String> subjects) {
        subjects_.addAll(subjects);
    }

    @Override
    public void setConnectionInfo(String info) {

    }

    /**
     * Sets the callback delegate {@link NetworkDataSinkDelegate} to call back with the data received and translated by
     * the provider.
     *
     * @param delegate The object implementing the {@link NetworkDataSinkDelegate} interface.
     */
    @Override
    public void setCallbackDelegate(NetworkDataSinkDelegate delegate) {
        if(delegate != null)
            callback_ = delegate;
    }

    /**
     * Called to start listening for incoming messages filtered by subject. This can only be called once per provider
     * instance.
     */
    @Override
    public void startListening() {
        if(subjects_.isEmpty())
            throw new NetworkException("Please set the kafka consumer topic before starting kafka client.");
        if(thread_ == null) {
            thread_ = new Thread(this);
            thread_.start();
        }
    }

    /**
     * Called to stop listening for incoming messages. This should only be called once and only after startListening
     * was called.
     */
    @Override
    public void stopListening() {
        if(running_.get()) {
            signalStop_.set(true);
            logger_.info("KafkaClient closing...");
            try {
                thread_.join(15_000); // 15 seconds for now, may make it configurable if needed.
            } catch(InterruptedException e) { /* Ignore interruption here */ }
            finally {
                thread_ = null;
            }
        }
    }

    /**
     * @return A flag to determine if the provider implemented is currently listening.
     */
    @Override
    public boolean isListening() {
        return running_.get();
    }

    /**
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
     * @return The name of the provider to be created by the {@link com.intel.networking.sink.NetworkDataSinkFactory}.
     */
    @Override
    public String getProviderName() {
        return "kafka";
    }

    @Override
    public void setStreamLocationCallback(StreamLocationHandler handler) {
    }

    @Override
    public void setLocationId(String id) {
    }

    @Override
    public void run() {
        logger_.info("Connecting to bootstrap.servers='%s', group.id='%s', schema.registry.url='%s', kafka-topic='%s'",
                args_.get("bootstrap_servers"), args_.get("group_id"), args_.get("schema_registry_url"), subjects_);
        Duration duration = Duration.ofSeconds(Long.parseLong(timeout_));
        try (KafkaConsumer<String, String> kafkaClient = createKafkaClient()) {
            kafkaClient.subscribe(subjects_);
            running_.set(true);
            logger_.info("KafkaClient connected.");
            while(!signalStop_.get()) {
                ConsumerRecords<String, String> receivedRecords = kafkaClient.poll(duration);
                for (ConsumerRecord<String, String> receivedRecord : receivedRecords) {
                    String topic = receivedRecord.topic();
                    String receivedData = receivedRecord.value();
                    logger_.debug("\n\n*** TOPIC='%s'; MESSAGE='%s'\n", receivedRecord.topic(), receivedData);
                    if (callback_ != null) {
                        callback_.processIncomingData(topic, receivedData);
                    }
                }
                kafkaClient.commitAsync();
            }
        } catch (Exception e) {
            logger_.error("Error while reading Kafka bus data: " + e.getMessage());
        } finally {
            running_.set(false);
            logger_.info("KafkaClient closed.");
        }
    }

    protected KafkaConsumer<String, String> createKafkaClient() { // Overridden for testing only.
        return new KafkaConsumer<>(kafkaProperties);
    }

    private NetworkDataSinkDelegate callback_;
    private Thread thread_;
    private Logger logger_;
    private String timeout_;
    private HashSet<String> subjects_ = new HashSet<>();

    private final Map<String, String> args_;
    private final Properties kafkaProperties = new Properties();
    final AtomicBoolean running_ = new AtomicBoolean(false);
    private final AtomicBoolean signalStop_ = new AtomicBoolean(false);

    private final String[] requiredKafkaProperties = {"bootstrap_servers", "group_id", "schema_registry_url"};
    private final String[] optionalKafkaProperties = {"enable_auto_commit", "reset_auto_offset", "is_avro"};

    private static final String KAFKA_BOOTSTRAP_SERVER = "bootstrap.servers";
    private static final String KAFKA_GROUP_ID = "group.id";
    private static final String KAFKA_SCHEMA_REG_URL = "schema.registry.url";
    private static final String KAFKA_AUTO_COMMIT = "auto.commit.enable";
    private static final String KAFKA_AUTO_OFFSET = "auto.offset.reset";
    private static final String KAFKA_IS_AVRO_FORMAT = "specific.avro.reader";
    private static final String KAFKA_KEY_DESERIALIZER = "key.deserializer";
    private static final String KAFKA_VALUE_DESERIALIZER = "value.deserializer";
    private static final String KAFKA_TIMEOUT = "timeout";
}
