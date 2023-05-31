package org.jembi.ciol.dhis2Exporter;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.http.javadsl.model.StatusCode;
import akka.http.javadsl.model.StatusCodes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.ciol.AppConfig;
import org.jembi.ciol.RestConfig;
import org.jembi.ciol.kafka.MyKafkaProducer;
import org.jembi.ciol.models.GlobalConstants;
import org.jembi.ciol.models.NotificationMessage;
import org.jembi.ciol.models.Payload;
import org.jembi.ciol.utils.AppUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import static org.jembi.ciol.utils.AppUtils.OBJECT_MAPPER;

public class BackEnd extends AbstractBehavior<BackEnd.Event> {
    private static final Logger LOGGER = LogManager.getLogger(BackEnd.class);
    private final MyKafkaProducer<String, NotificationMessage> notificationStream;
    private FrontEnd stream = null;

    private BackEnd(ActorContext<Event> context) {
        super(context);
        LOGGER.debug("new BackEnd");
        notificationStream = new MyKafkaProducer<>(AppConfig.KAFKA_CLIENT_ID, GlobalConstants.TOPIC_NOTIFICATIONS);
        LOGGER.debug("new KafkaProducer");
    }

    public static Behavior<Event> create() {
        LOGGER.info("Creating backend");
        return Behaviors.setup(BackEnd::new);
    }

    private static String getURL() {
        return String.format(Locale.ROOT, "http://%s:%d",
                             Main.getConfigDHIS2ip(),
                             Main.getConfigDHIS2port());
    }

    private void sendNotification(final String key, final String payload) {
        LOGGER.info("Send notification : {} {}", key, payload);
        notificationStream.produceAsync(key, new NotificationMessage(
                                                System.currentTimeMillis(),
                                                "Dispatcher",
                                                List.of(new NotificationMessage.Message("Admin",
                                                                                        "info",
                                                                                        System.currentTimeMillis(),
                                                                                        200,
                                                                                        "messageType",
                                                                                        payload,
                                                                                        "messagePath"))),
                                        (metadata, e) -> {
                                            if (e == null) {
                                                LOGGER.info("{} {}", metadata.timestamp(), metadata.offset());
                                            } else {
                                                LOGGER.error(e.getLocalizedMessage(), e);
                                            }
                                        });
    }

    private Behavior<Event> eventHandlerCheckConnected() {
        LOGGER.debug("Event handler - checking if connected!");
        if (!Main.haveConfig()) {
            LOGGER.debug("Not configured");
            if (!Main.getMissingConfigStatusSent()) {
                sendNotification("Not Configured", "No server configuration");
                Main.setMissingConfigStatusSent(true);
            }
            return Behaviors.same();
        }
        Main.setMissingConfigStatusSent(false);
        try {
            if (OKHTTP.getInstance().httpClient == null) {
                OKHTTP.getInstance().createAuthenticatedClient(Main.getConfigDHIS2username(),
                                                               Main.getConfigDHIS2password());
            }
            if (OKHTTP.getInstance().httpClient != null) {
                final var response = OKHTTP.getInstance().checkClientAvailability(getURL().concat("/healthcheck"));
                if (response.isSuccessful()) {
                    stream = new FrontEnd();
                    stream.open(getContext().getSystem(), getContext().getSelf());
                    response.close();
                    sendNotification("Connected", "dhis_server_up");
                    return stateConnected();
                }
                response.close();
            }
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return Behaviors.same();
    }

    private Behavior<Event> eventHandlerCheckDisconnected() {
        LOGGER.debug("Event handler check disconnected!");
        if (!Main.haveConfig()) {
            if (stream != null) {
                stream.close();
                stream = null;
            }
            if (!Main.getMissingConfigStatusSent()) {
                sendNotification("Not Configured", "No server configuration");
                Main.setMissingConfigStatusSent(true);
            }
            return stateDisconnected();
        }
        Main.setMissingConfigStatusSent(false);
        try {
            if (OKHTTP.getInstance().httpClient == null) {
                stream.close();
                stream = null;
                sendNotification("Disconnected", "dhis_server_down");
                return stateDisconnected();
            }
            final var response = OKHTTP.getInstance().checkClientAvailability(getURL().concat("/healthcheck"));
            if (!response.isSuccessful()) {
                stream.close();
                stream = null;
                sendNotification("Disconnected", "dhis_server_down");
                return stateDisconnected();
            }
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return Behaviors.same();
    }

    private Behavior<Event> eventHandlerStartCheckConnectionPeriodicTimer() {
        LOGGER.debug("Event handler - starting periodic time!");

        return Behaviors.withTimers(timers -> {
            timers.startTimerAtFixedRate("CheckConnectedTimer",
                                         EventCheckConnection.INSTANCE,
                                         Duration.ofSeconds(5),
                                         Duration.ofSeconds(300));
            return Behaviors.same();
        });
    }

    private Behavior<Event> eventHandlerSendToDHIS(EventSendToDHIS message) {
        LOGGER.info("{}", message);

        try {
            final var json = new String(AppUtils.OBJECT_MAPPER.writeValueAsBytes(message.payload));
            LOGGER.debug("{}", json);
            final var response = OKHTTP.getInstance().clientPost(getURL().concat("/dhis2Report"), json);
            final var body = response.body();
            final var jsonStr = body == null ? "{}" : new String(body.bytes());
            LOGGER.info("{}, {}", response.code(), jsonStr);
            if (response.code() / 100 != 2) {
                sendNotification("Error", String.format("dhis_server_error:%d", response.code()));
                message.replyTo.tell(new EventSendtoDHISRSP(false));
            } else {
                sendNotification("Dispatcher", String.format("dhis_server_sent:%s", jsonStr));
                message.replyTo.tell(new EventSendtoDHISRSP(true));
            }
            response.close();
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            message.replyTo.tell(new EventSendtoDHISRSP(false));
            stream.close();
            stream = null;
            sendNotification("Exception", String.format("dhis_server_exception:%s", e.getLocalizedMessage()));
            return stateDisconnected();
        }
        return Behaviors.same();
    }

    private Behavior<Event> eventSetConfigHandler(EventSetConfig event) {
        LOGGER.info("{}", event.restConfig);

        if ("dispatcher_service".equals(event.restConfig.appID())) {
            try {
                Main.setConfig(event.restConfig);
                OBJECT_MAPPER.writeValue(new File("/app/conf/myConfig.json"), event.restConfig);
                event.replyTo.tell(new EventSetConfigRsp(StatusCodes.OK));
            } catch (IOException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
                event.replyTo.tell(new EventSetConfigRsp(StatusCodes.IM_A_TEAPOT));
            }

        } else {
            event.replyTo.tell(new EventSetConfigRsp(StatusCodes.IM_A_TEAPOT));
        }

        return Behaviors.same();
    }

    public Receive<Event> stateConnected() {
        LOGGER.debug("stateConnected");
        ReceiveBuilder<Event> builder = newReceiveBuilder();
        return builder
                .onMessage(EventConnected.class, e -> Behaviors.same())
                .onMessage(EventCheckConnection.class, e -> eventHandlerCheckDisconnected())
                .onMessage(EventDisconnected.class, e -> stateDisconnected())
                .onMessage(EventSendToDHIS.class, this::eventHandlerSendToDHIS)
                .onMessage(EventSetConfig.class, this::eventSetConfigHandler)
                .build();
    }

    public Receive<Event> stateDisconnected() {
        LOGGER.debug("stateDisconnected");
        ReceiveBuilder<Event> builder = newReceiveBuilder();
        return builder
                .onMessage(EventConnected.class, e -> stateConnected())
                .onMessage(EventCheckConnection.class, e -> eventHandlerCheckConnected())
                .onMessage(EventDisconnected.class, e -> Behaviors.same())
                .onMessage(EventStartCheckConnectionPeriodicTimer.class,
                           e -> eventHandlerStartCheckConnectionPeriodicTimer())
                .onMessage(EventSetConfig.class, this::eventSetConfigHandler)
                .build();
    }

    @Override
    public Receive<Event> createReceive() {
        LOGGER.info("Create Receive");
        return stateDisconnected();
    }

    private enum EventConnected implements Event {
        INSTANCE
    }

    private enum EventDisconnected implements Event {
        INSTANCE
    }

    public enum EventCheckConnection implements Event {
        INSTANCE
    }

    public enum EventStartCheckConnectionPeriodicTimer implements Event {
        INSTANCE
    }

    interface Event { }

    interface EventResponse { }

    public record EventSendToDHIS(
            String key,
            Payload payload,
            ActorRef<EventSendtoDHISRSP> replyTo) implements Event {
    }

    public record EventSendtoDHISRSP(boolean result) implements EventResponse { }

    public record EventSetConfig(
            RestConfig restConfig,
            ActorRef<EventSetConfigRsp> replyTo) implements Event {
    }

    public record EventSetConfigRsp(StatusCode responseCode) {
    }
}
