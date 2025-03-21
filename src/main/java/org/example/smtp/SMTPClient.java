
package org.example.smtp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class SMTPClient {
    private static final String SMTP_SERVER = "localhost"; // Modifie si besoin
    private static final int SMTP_PORT = 26;  // Port SMTP standard
    private static final String SENDER = "test@example.COM";
    private static final String RECIPIENT = "recipient@example.COM";
    private static final int MIN_MESSAGE_SIZE = 1000;  // Taille minimale du message en octets
    private static final int MAX_MESSAGE_SIZE = 5000;  // Taille maximale (modifiable)
    private static final int DELAY_BETWEEN_EMAILS_MS = 2000; // Délai entre chaque email (2 sec)

    public static void main(String[] args) {
        while (true) { // Envoi en boucle
            try {
                sendEmail();
                Thread.sleep(DELAY_BETWEEN_EMAILS_MS); // Pause avant le prochain email
            } catch (InterruptedException e) {
                System.err.println("Interruption détectée. Arrêt du client.");
                break;
            }
        }
    }

    private static void sendEmail() {
        try (Socket socket = new Socket(SMTP_SERVER, SMTP_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            System.out.println("📡 Connecté au serveur SMTP");

            // Lecture de la bannière
            System.out.println("📩 " + reader.readLine());

            // HELO
            writer.println("HELO example.com");
            System.out.println("📩 " + reader.readLine());

            // MAIL FROM
            writer.println("MAIL FROM:" + SENDER + "");
            System.out.println("📩 " + reader.readLine());

            // RCPT TO
            writer.println("RCPT TO:" + RECIPIENT + "");
            System.out.println("📩 " + reader.readLine());

            // DATA
            writer.println("DATA");
            System.out.println("📩 " + reader.readLine());

            // Générer un corps de message de taille aléatoire (min 1000 octets)
            String messageBody = generateRandomMessage();
            writer.println("Subject: Test Email");
            writer.println("From: " + SENDER);
            writer.println("To: " + RECIPIENT);
            writer.println(); // Séparation headers / body
            writer.println(messageBody);
            writer.println("."); // Fin du message

            // Lire la réponse du serveur
            System.out.println("📩 " + reader.readLine());

            // QUIT
            writer.println("QUIT");
            System.out.println("📩 " + reader.readLine());

            System.out.println("✅ Email envoyé avec succès !");

        } catch (IOException e) {
            System.err.println("❌ Erreur d'envoi d'email : " + e.getMessage());
        }
    }

    private static String generateRandomMessage() {
        Random random = new Random();
        int messageSize = MIN_MESSAGE_SIZE + random.nextInt(MAX_MESSAGE_SIZE - MIN_MESSAGE_SIZE);
        StringBuilder message = new StringBuilder(messageSize);

        // Générer des caractères aléatoires
        for (int i = 0; i < messageSize; i++) {
            message.append((char) ('A' + random.nextInt(26))); // Lettres aléatoires A-Z
        }

        return message.toString();
    }
}
