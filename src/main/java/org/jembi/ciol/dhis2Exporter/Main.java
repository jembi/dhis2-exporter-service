package org.jembi.ciol.dhis2Exporter;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.ciol.AppConfig;
import org.jembi.ciol.RestConfig;

import java.io.File;
import java.io.IOException;

import static org.jembi.ciol.utils.AppUtils.OBJECT_MAPPER;

public final class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    private static final String NO_CONFIG_MESSAGE = "No Configuration";
    private static boolean missingConfigStatusSent = false;
    private static RestConfig restConfig = null;
    private HttpServer httpServer;

    private Main() { }

    public static boolean getMissingConfigStatusSent() {
        return missingConfigStatusSent;
    }

    public static void setMissingConfigStatusSent(final boolean missingConfigStatusSent) {
        Main.missingConfigStatusSent = missingConfigStatusSent;
    }

    public static String getConfigDHIS2ip() {
        if (restConfig == null) {
            LOGGER.warn(NO_CONFIG_MESSAGE);
            throw new NullPointerException(NO_CONFIG_MESSAGE);
        }
        return Main.restConfig.dhis2IP();
    }

    public static Long getConfigDHIS2port() {
        if (restConfig == null) {
            LOGGER.warn(NO_CONFIG_MESSAGE);
            throw new NullPointerException(NO_CONFIG_MESSAGE);
        }
        return Main.restConfig.dhis2Port();
    }

    public static String getConfigDHIS2username() {
        if (restConfig == null) {
            LOGGER.warn(NO_CONFIG_MESSAGE);
            throw new NullPointerException(NO_CONFIG_MESSAGE);
        }
        return Main.restConfig.dhis2Username();
    }

    public static String getConfigDHIS2password() {
        LOGGER.warn(NO_CONFIG_MESSAGE);
        if (restConfig == null) {
            throw new NullPointerException(NO_CONFIG_MESSAGE);
        }
        return Main.restConfig.dhis2Password();
    }

    public static void setConfig(final RestConfig restConfig) {
        Main.restConfig = restConfig;
    }

    public static boolean haveConfig() {
        return Main.restConfig != null;
    }

    public static void main(final String[] args) {
        new Main().run();
    }

    public Behavior<Void> create() {
        return Behaviors.setup(
                context -> {
                    ActorRef<BackEnd.Event> backEnd = context.spawn(BackEnd.create(), "BackEnd");
                    context.watch(backEnd);
                    backEnd.tell(BackEnd.EventStartCheckConnectionPeriodicTimer.INSTANCE);
                    httpServer = new HttpServer();
                    httpServer.open(context.getSystem(), backEnd);
                    return Behaviors.receive(Void.class)
                                    .onSignal(akka.actor.typed.Terminated.class, sig -> Behaviors.stopped())
                                    .build();
                });
    }

    private void run() {
        LOGGER.info("KAFKA: {} {} {}",
                    AppConfig.KAFKA_BOOTSTRAP_SERVERS,
                    AppConfig.KAFKA_APPLICATION_ID,
                    AppConfig.KAFKA_CLIENT_ID);
        try {
            restConfig = OBJECT_MAPPER.readValue(new File("/app/conf/myConfig.json"), RestConfig.class);
        } catch (IOException e) {
            LOGGER.warn("No configuration loaded");
            restConfig = null;
        }
        ActorSystem.create(this.create(), "DispatchApp");
    }
}