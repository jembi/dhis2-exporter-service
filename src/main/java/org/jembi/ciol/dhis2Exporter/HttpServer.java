package org.jembi.ciol.dhis2Exporter;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import ch.megard.akka.http.cors.javadsl.settings.CorsSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.ciol.AppConfig;
import org.jembi.ciol.RestConfig;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static ch.megard.akka.http.cors.javadsl.CorsDirectives.cors;

class HttpServer extends AllDirectives {
    private static final Logger LOGGER = LogManager.getLogger(HttpServer.class);
    private CompletionStage<ServerBinding> binding = null;

    public static String readJsonFile(final String fileName) {
        String jsonData = "";

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                jsonData += line + "\n";
            }
            bufferedReader.close();
        } catch (Exception e) {
            LOGGER.error("Couldn't read configuration file");
            e.printStackTrace();
        }
        return jsonData;
    }

    void close(ActorSystem<Void> system) {
        binding.thenCompose(ServerBinding::unbind)
               .thenAccept(unbound -> system.terminate());
    }

    void open(final ActorSystem<Void> system,
              final ActorRef<BackEnd.Event> backEnd) {
        final Http http = Http.get(system);
        HttpServer app = new HttpServer();
        binding = http.newServerAt(AppConfig.HTTP_SERVER_HOST,
                                   AppConfig.HTTP_SERVER_PORT)
                      .bind(app.createRoute(system, backEnd));
        LOGGER.debug("{}", binding);
        LOGGER.debug("{}", binding.toString());
        binding.whenComplete((v1, v2) -> LOGGER.debug("{},{}", v1.localAddress(), v2.getCause()));
        LOGGER.info("Server online at http://{}:{}", AppConfig.HTTP_SERVER_HOST, AppConfig.HTTP_SERVER_PORT);
    }

    private Route setConfig(final ActorSystem<Void> actorSystem,
                           final ActorRef<BackEnd.Event> backEnd,
                           final RestConfig config) {
        LOGGER.debug("{}",config);

        CompletionStage<BackEnd.EventSetConfigRsp> result =
                AskPattern.ask(backEnd,
                               replyTo -> new BackEnd.EventSetConfig(config, replyTo),
                               java.time.Duration.ofSeconds(11),
                               actorSystem.scheduler());
        var completableFuture = result.toCompletableFuture();
        try {
            var reply = completableFuture.get(21, TimeUnit.SECONDS);
            return complete(reply.responseCode());
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
        }
        return complete(StatusCodes.IM_A_TEAPOT);
    }

    private Route createRoute(final ActorSystem<Void> actorSystem,
                              final ActorRef<BackEnd.Event> backEnd) {
        final var settings = CorsSettings.defaultSettings().withAllowGenericHttpRequests(true);
        return cors(settings,
                () -> pathPrefix("CIOL",
                        () -> concat(
                                post(() -> concat(
                                        path("installNewConfig",
                                                () -> entity(Jackson.unmarshaller(RestConfig.class),
                                                        config -> setConfig(actorSystem, backEnd, config)))
                                )),
                                get(() -> concat(
                                        path("dispatchconfig",
                                                () -> {
                                                    final String jsonConfig = readJsonFile(
                                                            "/app/conf/myConfig.json");
                                                    return complete(jsonConfig);
                                                })
                                ))

                        )));
    }
}

