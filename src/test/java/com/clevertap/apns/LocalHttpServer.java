package com.clevertap.apns;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalHttpServer {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    HttpServer httpServer;
    int port;
    String LOCAL_HOST = "http://127.0.0.1";
    String CONTEXT = "/serveRequest";

    public static int nextFreePort() {
        try {
            try (ServerSocket tempSocket = new ServerSocket(0)) {
                return tempSocket.getLocalPort();
            }
        } catch (IOException e) {
            return -1;
        }
    }

    public int init() throws Exception {
        port = nextFreePort();
        baseConfig(port);
        return port;
    }

    public String getUrl() {
        return LOCAL_HOST + ":" + port + CONTEXT;
    }

    public void baseConfig(int port) throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext(CONTEXT, new RequestHandler());
        httpServer.setExecutor(executorService); // creates a default executor
        httpServer.start();
        System.out.println("Local Http server created on port " + port);
    }

    public void shutDownServer() {
        httpServer.stop(1);
        executorService.shutdown();
    }

    static class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "Request Executed";
            t.getResponseHeaders().set("Content-Type", "text/plain");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}