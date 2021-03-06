package no.nav.perftest;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Signature;

public class VerifySignatureTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Got to main in: " + ManagementFactory.getRuntimeMXBean().getUptime() + " ms.");
        new Thread(() -> {
            try {
                HttpServer httpServer = HttpServer.create(new InetSocketAddress(8000), 0);
                httpServer.createContext("/is_alive", exchange -> {
                    byte[] response = "I'm alive".getBytes("UTF-8");
                    exchange.sendResponseHeaders(200, response.length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(response);
                    outputStream.close();
                });
                httpServer.setExecutor(null);
                httpServer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        long mainStart = System.currentTimeMillis();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(Files.newInputStream(Paths.get(args[0])), args[1].toCharArray());

        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initVerify(keyStore.getCertificate("perftest-public"));

        byte[] signedContent = Files.readAllBytes(Paths.get(args[2]));
        byte[] signatureBytes = Files.readAllBytes(Paths.get(args[3]));

        System.out.println("Ready state in " + (System.currentTimeMillis() - mainStart) + " ms.");
        System.out.println("Warming up the JVM to avoid JIT compilation issues, would kinda be unfair...");

        long warmupStart = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            signature.update(signedContent);
            if (!signature.verify(signatureBytes))
                throw new RuntimeException("Invalid signature found?!");
        }
        System.out.println("Warmup done in " + (System.currentTimeMillis() - warmupStart) + " ms.");

        System.out.println("Starting real run...");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            signature.update(signedContent);
            if (!signature.verify(signatureBytes))
                throw new RuntimeException("Invalid signature found?!");
        }
        System.out.println("Real run done in " + (System.currentTimeMillis() - startTime) + " ms.");
    }
}
