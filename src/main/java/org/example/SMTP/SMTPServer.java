package org.example.SMTP;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SMTPServer {
    private static final int PORT = 25;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur SMTP démarré sur le port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new SMTPClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
