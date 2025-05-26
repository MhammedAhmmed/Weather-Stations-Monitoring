package org.example;

import org.example.kv.KVStore;
import org.example.model.Key;
import org.example.model.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class QueryServer implements Runnable {

    private final KVStore<Key> kvStore;
    private final int port;

    public QueryServer(KVStore<Key> kvStore, int port) {
        this.kvStore = kvStore;
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Bitcask query server started on port " + port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)) {

                    handleClientRequest(in, out);
                } catch (IOException e) {
                    System.err.println("Error handling client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start Bitcask query server: " + e.getMessage());
        }
    }

    private void handleClientRequest(BufferedReader in, PrintWriter out) throws IOException {
        String command = in.readLine();
        if (command == null) return;

        if ("--view-all".equals(command)) {
            exportAllToCsv(out);
        } else if (command.startsWith("--view --key=")) {
            String key = command.substring("--view --key=".length());
            viewSingleKey(out, key);
        } else if (command.startsWith("--perf-test")) {
            handlePerformanceTest(out);
        }
        else {
            out.println("Error: Unknown command");
        }
    }

    private void handlePerformanceTest(PrintWriter out) {
        out.println("key,value");
        kvStore.getKeyDirectory().getEntryByKey().keySet().forEach(key -> {
            byte[] value = kvStore.get(key);
            Message message = Message.deserialize(value);
            out.printf("\"%s\",\"%s\"%n", key.toString(), message.toString());
        });
    }

    private void viewSingleKey(PrintWriter out, String keyStr) {
        try {
            Key key = new Key(keyStr); // Assuming Key has a String constructor
            byte[] value = kvStore.get(key);

            if (value != null) {
                Message message = Message.deserialize(value);
                out.println("Key: " + key);
                out.println("Value: " + message);
            } else {
                out.println("Error: Key not found");
            }
        } catch (Exception e) {
            out.println("Error: Invalid key format");
        }
    }

    private void exportAllToCsv(PrintWriter out) {
        out.println("key,value");
        kvStore.getKeyDirectory().getEntryByKey().keySet().forEach(key -> {
            byte[] value = kvStore.get(key);
            Message message = Message.deserialize(value);
            out.printf("\"%s\",\"%s\"%n", key.toString(), message.toString());
        });
    }
}