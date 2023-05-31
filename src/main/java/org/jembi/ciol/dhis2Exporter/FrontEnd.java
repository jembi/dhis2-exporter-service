package org.jembi.ciol.dhis2Exporter;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.ciol.AppConfig;
import org.jembi.ciol.models.GlobalConstants;
import org.jembi.ciol.models.Payload;
import org.jembi.ciol.serdes.JsonPojoDeserializer;
import org.jembi.ciol.serdes.JsonPojoSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FrontEnd {
    private static final Logger LOGGER = LogManager.getLogger(FrontEnd.class);
    private KafkaStreams dhisStream;

    FrontEnd() {
        LOGGER.info("FrontEndStream constructor");
    }

    void sendMessage(ActorSystem<Void> system,
                     final ActorRef<BackEnd.Event> backEnd,
                     String key,
                     Payload payload) {
        CompletionStage<BackEnd.EventSendtoDHISRSP> result =
                AskPattern.ask(
                        backEnd,
                        replyTo -> new BackEnd.EventSendToDHIS(key, payload, replyTo),
                        java.time.Duration.ofSeconds(10),
                        system.scheduler());
        var completableFuture = result.toCompletableFuture();
        try {
            var reply = completableFuture.get(15, TimeUnit.SECONDS);
            if (reply != null) {
                if (reply.result()) {
                    LOGGER.info("Send to DHIS2 successful!");
                } else {
                    LOGGER.error("BACK END RESPONSE(ERROR)");
                }
            } else {
                LOGGER.error("Incorrect class response");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public void open(final ActorSystem<Void> system,
                     final ActorRef<BackEnd.Event> backEnd) {
        LOGGER.info("Dispatch Stream Processor");

        final Properties props = loadConfig();
        final Serde<String> stringSerde = Serdes.String();

        final Serializer<Payload> pojoSerializer = new JsonPojoSerializer<>();
        final Deserializer<Payload> pojoDeserializer = new JsonPojoDeserializer<>();
        final Map<String, Object> serdeProps = new HashMap<>();
        serdeProps.put(JsonPojoDeserializer.CLASS_TAG, Payload.class);
        pojoDeserializer.configure(serdeProps, false);
        final Serde<Payload> dhisPayloadSerde = Serdes.serdeFrom(pojoSerializer, pojoDeserializer);

        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final KStream<String, Payload> messageStream = streamsBuilder.stream(
                GlobalConstants.TOPIC_PAYLOAD_QUEUE,
                Consumed.with(stringSerde, dhisPayloadSerde));

        messageStream.foreach((key, entity) -> {
            LOGGER.info("{}",entity);
            sendMessage(system, backEnd, key, entity);
        });

        dhisStream = new KafkaStreams(streamsBuilder.build(), props);
        dhisStream.cleanUp();
        dhisStream.start();
        LOGGER.info("KafkaStreams started");
    }

    public void close() {
        LOGGER.warn("Stream closed");
        dhisStream.close();
        dhisStream = null;
    }

    private Properties loadConfig() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, AppConfig.KAFKA_APPLICATION_ID);
        props.put(StreamsConfig.CLIENT_ID_CONFIG, AppConfig.KAFKA_CLIENT_ID);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, AppConfig.KAFKA_BOOTSTRAP_SERVERS);
        return props;
    }
}
