package com.intel.networking.sink.kafka;

import com.intel.logging.Logger;
import com.intel.networking.NetworkException;
import com.intel.networking.sink.NetworkDataSinkDelegate;
import com.intel.networking.sink.NetworkDataSinkEx;
import com.intel.networking.sink.StreamLocationHandler;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
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
     *   key.deserializer     - (def: StringDeserializer) To deserialize received key messages of any Avro/string type
     *                          from Kafka.
     *   value.deserializer   - (def: StringDeserializer) To deserialize received value messages of any Avro/json type
     *                          from Kafka. All topics in the single instance MUST use the same value deserializer.
     *   topics               - (req) Comma separated list of kafka topics to subscribe to.
     *   timeout              - (def: 60) Timeout in seconds for kafka connections.
     *
     *   NOTE: Any other Kafka consumer properties are allowed here as well.
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
        setMonitoringSubjects(Arrays.asList(args_.get(KAFKA_TOPICS).split(",")));
        args_.remove(KAFKA_TOPICS);
        timeout_ = args_.getOrDefault(KAFKA_TIMEOUT, args_.getOrDefault(KAFKA_TIMEOUT, "60"));
        args_.remove(KAFKA_TIMEOUT);
        args_.put(KAFKA_SPECIFIC_AVRO_READER, "false");
        args_.putIfAbsent(KAFKA_KEY_DESERIALIZER, "org.apache.kafka.common.serialization.StringDeserializer");
        args_.putIfAbsent(KAFKA_VALUE_DESERIALIZER, "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProperties.putAll(args_);
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
        Duration duration = Duration.ofSeconds(Long.parseLong(timeout_));
        KafkaConsumer<String, Object> kafkaClient = null;
        while(!signalStop_.get()) {
            logger_.info("%sonnecting to %s='%s', %s='%s', %s='%s', topics='%s'", kafkaClient==null?"C":"Re-c",
                    KAFKA_BOOTSTRAP_SERVER, args_.get(KAFKA_BOOTSTRAP_SERVER), KAFKA_GROUP_ID,
                    args_.get(KAFKA_GROUP_ID), KAFKA_SCHEMA_REG_URL, args_.get(KAFKA_SCHEMA_REG_URL), subjects_);
            try {
                kafkaClient = createKafkaClient();
                kafkaClient.subscribe(subjects_);
                running_.set(true);
                logger_.info("KafkaClient connected.");
                while (!signalStop_.get()) {
                    ConsumerRecords<String, Object> receivedRecords = kafkaClient.poll(duration);
                    for (ConsumerRecord<String, Object> receivedRecord : receivedRecords) {
                        String topic = receivedRecord.topic();
                        Object receivedData = receivedRecord.value();
                        logger_.debug("\n\n*** TOPIC='%s'; MESSAGE='%s'\n", receivedRecord.topic(),
                                receivedData.toString());
                        if (callback_ != null)
                            callback_.processIncomingData(topic, receivedData.toString());
                    }
                    kafkaClient.commitAsync();
                }
            } catch (Exception e) {
                logger_.exception(e, "Error while reading Kafka bus data");
            } finally {
                if(kafkaClient != null) {
                    kafkaClient.close();
                    logger_.info("KafkaClient closed.");
                }
            }
        }
        running_.set(false);
    }

    protected KafkaConsumer<String, Object> createKafkaClient() { // Overridden for testing only.
        return new KafkaConsumer<>(kafkaProperties);
    }

    private NetworkDataSinkDelegate callback_;
    private Thread thread_;
    private Logger logger_;
    private String timeout_;
    private final HashSet<String> subjects_ = new HashSet<>();

    private final Map<String, String> args_;
    private final Properties kafkaProperties = new Properties();
    final AtomicBoolean running_ = new AtomicBoolean(false);
    private final AtomicBoolean signalStop_ = new AtomicBoolean(false);

    private static final String KAFKA_BOOTSTRAP_SERVER = "bootstrap.servers";
    private static final String KAFKA_GROUP_ID = "group.id";
    private static final String KAFKA_SCHEMA_REG_URL = "schema.registry.url";
    private static final String KAFKA_KEY_DESERIALIZER = "key.deserializer";
    private static final String KAFKA_VALUE_DESERIALIZER = "value.deserializer";
    private static final String KAFKA_TIMEOUT = "timeout";
    private static final String KAFKA_TOPICS = "topics";
    private static final String KAFKA_SPECIFIC_AVRO_READER = "specific.avro.reader";
    private static final String[] requiredKafkaProperties = {KAFKA_BOOTSTRAP_SERVER, KAFKA_GROUP_ID,
            KAFKA_SCHEMA_REG_URL, KAFKA_TOPICS};
}
