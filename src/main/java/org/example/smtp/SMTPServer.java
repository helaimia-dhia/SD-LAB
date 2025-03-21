package org.example.smtp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SMTPServer {
    private static final int PORT = 25;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("SMTP Server is listening on port " + PORT);


            while (true) {
                System.out.println(" En attente d'un client...");
                Socket socket = serverSocket.accept(); // Bloque jusqu'à une connexion
                System.out.println(" Client connecté !");

                // Gérer le client dans le même thread (une seule connexion à la fois)
                SMTPHandler handler = new SMTPHandler(socket);
                handler.run(); // Exécuter directement sans thread

                System.out.println("Client déconnecté. Attente du prochain...");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
