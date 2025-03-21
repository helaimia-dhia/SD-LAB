package org.example.pop3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class POP3Server {
    private static final int PORT = 110;

    public static void main(String[] args) {
        //DatabaseManager.initializeDatabase(); // Initialiser la base de données

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println(" POP3 Server is listening on port " + PORT);

            while (true) {
                System.out.println(" En attente d'un client...");
                Socket clientSocket = serverSocket.accept(); // Bloque jusqu'à une connexion
                System.out.println(" Client connecté !");

                // Gérer le client dans le même thread (une seule connexion à la fois)
                POP3Handler handler = new POP3Handler(clientSocket);
                handler.run(); // Appel direct sans thread

                System.out.println(" Client déconnecté. Attente du prochain...");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
