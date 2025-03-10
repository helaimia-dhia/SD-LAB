package org.example.POP3;



import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class POP3Server {
    private static final int PORT = 110;  // Port standard pour le protocole POP3

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("POP3 Server started on port " + PORT);

            // Gérer une seule connexion à la fois
            Socket clientSocket = serverSocket.accept();  // Accepter une nouvelle connexion
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            // Gérer le client sans thread
            POP3ClientHandler clientHandler = new POP3ClientHandler(clientSocket);
            clientHandler.run();  // Appeler run() directement pour gérer la connexion

            System.out.println("Client disconnected. Shutting down server.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
