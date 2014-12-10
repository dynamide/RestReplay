package org.dynamide.restreplay.server;

import java.io.*;
import java.net.*;

import com.sun.net.httpserver.*;

public class EmbeddedServer {
    private HttpServer server;

    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(28080), 0);
        server.createContext("/", new SelfTestHttpHandler(this));
        server.setExecutor(null);
        server.start();
    }

    public void stopServer(){
        server.stop(0); //param is "delay"
    }

    public static void main(String[] args) throws IOException {
        new EmbeddedServer().startServer();
    }
}
