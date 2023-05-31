package org.jembi.ciol.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.ciol.AppConfig;
import org.jembi.ciol.serdes.JsonPojoSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Set up the Kafka Producer
public class MyKafkaProducer<KEY_TYPE, VAL_TYPE> {

    private static final Logger LOGGER = LogManager.getLogger(MyKafkaProducer.class);
    // mTopic is the name of the topic that we are writing 
    private final String mTopic;
    private final Producer<KEY_TYPE, VAL_TYPE> mProducer;

    public MyKafkaProducer(final String clientId, final String topic) {
        final Properties properties = new Properties();
        final String serializerClassName = JsonPojoSerializer.class.getName();

        mTopic = topic;

        // establish properties for KafkaProducer
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, AppConfig.KAFKA_BOOTSTRAP_SERVERS);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);

        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializerClassName);

        properties.put(ProducerConfig.ACKS_CONFIG, "1");
        properties.put(ProducerConfig.RETRIES_CONFIG, 3);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        mProducer = new KafkaProducer<>(properties);
    }

    public final void close() {
        LOGGER.debug("close producer for {}", mTopic);
        mProducer.close();
    }

    // Producer for Synchronous data
    public final RecordMetadata produceSync(final KEY_TYPE key, final VAL_TYPE item) throws ExecutionException,
            InterruptedException, TimeoutException {
        final ProducerRecord<KEY_TYPE, VAL_TYPE> rec = new ProducerRecord<>(mTopic, key, item);
        return mProducer.send(rec).get(10, TimeUnit.SECONDS);
    }

    // Producer for Asynchronous data - includes a Callback
    public final void produceAsync(final KEY_TYPE key, final VAL_TYPE item, final Callback callback) {
        final ProducerRecord<KEY_TYPE, VAL_TYPE> rec = new ProducerRecord<>(mTopic, key, item);
        mProducer.send(rec, callback);
    }
}
