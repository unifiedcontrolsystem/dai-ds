package com.intel.networking.source.kafka;

import com.intel.logging.Logger;
import com.intel.networking.NetworkException;
import com.intel.networking.source.NetworkDataSourceEx;
import com.intel.networking.source.NetworkDataSourceFactory;
import com.intel.properties.PropertyMap;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;


public class NetworkDataSourceKafka implements NetworkDataSourceEx {

    /**
     * Used by the {@link NetworkDataSourceFactory} to create an instance of the Kafka provider.
     *
     * @param args A Map<String,String> where the following values are recognized:
     *
     * bootstrap.servers   - (req)     To establish an initial connection to the Kafka cluster.
     * schema.registry.url - (req)     Registered schema URL the provider is expecting message to send to.
     * key.serializer     - (def: StringSerializer) To serialize key messages of any Avro/string
     *                          and send to Kafka.
     * value.serializer   - (def: StringSerializer) To serialize value messages of any Avro/json type and send
     *            to Kafka. All topics in the single instance MUST use the same value serializer.
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
            if(args_.get(requiredKafkaProperty) == null || args_.get(requiredKafkaProperty).isEmpty())
                throw new NetworkException("Given argument value cannot be null or empty. argument: '" + requiredKafkaProperty + "'");
        }

        kafkaProperties.putAll(args_);
        kafkaProperties.setProperty(KAFKA_BOOTSTRAP_SERVER, args_.get(KAFKA_BOOTSTRAP_SERVER));
        kafkaProperties.setProperty(KAFKA_SCHEMA_REG_URL, args_.get(KAFKA_SCHEMA_REG_URL));
        kafkaProperties.setProperty(KAFKA_KEY_SERIALIZER, StringSerializer.class.getName());
        kafkaProperties.setProperty(KAFKA_VALUE_SERIALIZER, StringSerializer.class.getName());
        isAvro = Boolean.parseBoolean(args_.getOrDefault(IS_AVRO, "false"));
        if(isAvro)
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
        kafkaProducer_ = new KafkaProducer<>(kafkaProperties);
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
    public boolean sendMessage(final String subject, final String message) {
        try {
            if(isAvro)
                sendAvroMessage(subject, message);
            else
                sendJsonMessage(subject, message);
        } catch (Exception e) {
            logger_.error(e.getMessage());
            throw new NetworkException(e.getMessage());
        }
        return true;
    }

    private void sendJsonMessage(final String subject, final String message) {
        kafkaProducer_.send(new ProducerRecord<>(subject, message));
        kafkaProducer_.flush();
    }

    private void sendAvroMessage(final String subject, final String message) {
        String avroSchema = getPublisherProperty(STREAM_ID).toString();
        assert avroSchema != null;
        Schema schema = readSchema(avroSchema);
        GenericRecord record = convertJsonToRecord(message, schema);
        kafkaProducer_.send(new ProducerRecord<>(subject, null, record));
        kafkaProducer_.flush();
    }

    private static Schema readSchema(String avroSchema) {
        return new Schema.Parser().parse(avroSchema);
    }

    private static GenericRecord convertJsonToRecord(String json, Schema schema) {
        try {
            DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
            GenericRecord reuse = new GenericData.Record(schema);
            return reader.read(reuse, DecoderFactory.get().jsonDecoder(schema, json));
        } catch (IOException | AvroRuntimeException e) {
            throw new SerializationException(
                    String.format("Error deserializing json %s to Avro of schema %s", json, schema), e);
        }
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

    @Override
    public void setPublisherProperty(String property, Object value) {
        topicsAvroInfo.put(property, value);
    }

    @Override
    public Object getPublisherProperty(String property) {
        return topicsAvroInfo.getStringOrDefault(property, null);
    }

    private Logger logger_;
    private Producer<String, Object> kafkaProducer_;
    private final static PropertyMap topicsAvroInfo = new PropertyMap();

    private final Map<String,String> args_;
    private final Properties kafkaProperties = new Properties();

    private boolean isAvro;

    private static final String STREAM_ID = "STREAM_ID";
    private static final String KAFKA_KEY_SERIALIZER = "key.serializer";
    private static final String KAFKA_VALUE_SERIALIZER = "value.serializer";
    private static final String KAFKA_BOOTSTRAP_SERVER = "bootstrap.servers";
    private static final String KAFKA_SCHEMA_REG_URL = "schema.registry.url";
    private static final String KAFKA_ACK = "acks";
    private static final String KAFKA_RETRY = "retries";
    private static final String IS_AVRO = "is_avro";

    private static final String[] requiredKafkaProperties = {KAFKA_BOOTSTRAP_SERVER, KAFKA_SCHEMA_REG_URL, IS_AVRO};
}
