package org.example.smtp;

import java.io.*;
import java.net.Socket;

public class SMTPHandler extends Thread {
    private Socket socket;
    private SMTPStateMachine stateMachine;

    public SMTPHandler(Socket socket) {
        this.socket = socket;
        this.stateMachine = new SMTPStateMachine();
    }

    public void run() {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             PrintWriter writer = new PrintWriter(output, true)) {

            writer.println("220 Welcome to SMTP Server");

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received: " + line);

                // 🔥 Convertir la commande en majuscules pour l'uniformisation
                String response = stateMachine.processCommand(line.trim().toUpperCase());

                if (response != null) {
                    writer.println(response);
                }

                if (stateMachine.getCurrentState() == SMTPState.QUIT) {
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
