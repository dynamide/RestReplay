package org.dynamide.restreplay.server;

import java.io.*;
import java.net.*;

import com.sun.net.httpserver.*;
import org.dynamide.util.Tools;

public class EmbeddedServer {
    public static final int DEFAULT_PORT = 28080;

    private HttpServer server;

    public void startServer(String port) throws IOException {
        int iPort = DEFAULT_PORT;
        if (Tools.notBlank(port)){
            iPort = Integer.parseInt(port);
        }
        server = HttpServer.create(new InetSocketAddress(iPort), 0);
        server.createContext("/", new SelfTestHttpHandler(this));
        server.setExecutor(null);
        server.start();
    }

    public void stopServer(){
        server.stop(0); //param is "delay"
    }

    public static void main(String[] args) throws IOException {
        new EmbeddedServer().startServer(""+DEFAULT_PORT);
    }
}
