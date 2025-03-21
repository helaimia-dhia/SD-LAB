package org.example.pop3;

import java.io.*;
import java.net.Socket;

public class POP3Handler extends Thread {
    private Socket socket;
    private POP3StateMachine stateMachine;

    public POP3Handler(Socket socket) {
        this.socket = socket;
        this.stateMachine = new POP3StateMachine();
    }

    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            writer.println("+OK POP3 server ready");

            String command;
            while ((command = reader.readLine()) != null) {
                System.out.println(" Reçu: " + command);
                String response = stateMachine.processCommand(command);
                writer.println(response);

                if (response.startsWith("+OK POP3 server signing off")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println(" Connexion interrompue de manière inattendue !");
        } finally {
            handleDisconnection(); //  Restaurer les emails et supprimer trash/
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(" Client déconnecté. Attente du prochain...");
        }
    }

    private void handleDisconnection() {
        System.out.println("⚠️ Connexion interrompue. Restauration des emails supprimés...");
        stateMachine.restoreDeletedEmails();
        stateMachine.deleteMarkedEmails();
    }
}
