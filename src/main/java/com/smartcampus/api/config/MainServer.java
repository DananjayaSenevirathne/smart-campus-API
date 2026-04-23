package com.smartcampus.api.config;

import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public final class MainServer {

    private static final URI BASE_URI = URI.create("http://0.0.0.0:8080/api/v1/");

    private MainServer() {
    }

    public static HttpServer startServer() {
        ResourceConfig config = new ApplicationConfig();
        return GrizzlyHttpServerFactory.createHttpServer(BASE_URI, config);
    }

    public static void main(String[] args) throws InterruptedException {
        HttpServer server = startServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

        Thread.currentThread().join();
    }
}
