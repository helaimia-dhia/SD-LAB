package org.example.SMTP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Pattern;

public class SMTPClientHandler extends Thread {
    private Socket socket;
    private enum State { HELO, MAIL_FROM, RCPT_TO, DATA, QUIT }
    private State currentState = State.HELO;
    private StringBuilder emailData = new StringBuilder();  // Pour stocker le corps de l'email
    private String recipient = null;

    // Expression régulière pour valider les adresses email SANS chevrons
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^<>\\s]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    public SMTPClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Réponse de bienvenue
            out.println("220 Welcome to SMTP Server");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received: " + inputLine);

                // === Gérer QUIT dans tous les états ===
                if (inputLine.equalsIgnoreCase("QUIT")) {
                    out.println("221 Bye");
                    socket.close();  // Fermer la connexion
                    return;
                }

                switch (currentState) {
                    // === ÉTAT HELO ===
                    case HELO:
                        if (inputLine.trim().equalsIgnoreCase("HELO")) {
                            out.println("250 Hello");
                            currentState = State.MAIL_FROM;
                        } else {
                            out.println("503 Bad sequence of commands");
                        }
                        break;

                    // === ÉTAT MAIL_FROM ===
                    case MAIL_FROM:
                        if (inputLine.startsWith("MAIL FROM:")) {
                            String from = inputLine.substring(10).trim();
                            if (EMAIL_PATTERN.matcher(from).matches()) {
                                out.println("250 OK");
                                currentState = State.RCPT_TO;
                            } else {
                                out.println("501 Syntax error in parameters or arguments");
                            }
                        } else {
                            out.println("503 Bad sequence of commands");
                        }
                        break;

                    // === ÉTAT RCPT_TO ===
                    case RCPT_TO:
                        if (inputLine.startsWith("RCPT TO:")) {
                            String to = inputLine.substring(8).trim();
                            if (EMAIL_PATTERN.matcher(to).matches()) {
                                recipient = to;
                                out.println("250 OK");
                            } else {
                                out.println("501 Syntax error in parameters or arguments");
                            }
                        } else if (inputLine.equalsIgnoreCase("DATA")) {
                            if (recipient != null) {
                                out.println("354 Start mail input; end with <CRLF>.<CRLF>");
                                currentState = State.DATA;
                            } else {
                                out.println("503 Bad sequence of commands");
                            }
                        } else {
                            out.println("503 Bad sequence of commands");
                        }
                        break;

                    // === ÉTAT DATA ===
                    case DATA:
                        if (inputLine.equals(".")) {
                            out.println("250 OK");
                            EmailStorage.storeEmail(recipient, emailData.toString());
                            emailData.setLength(0);
                            currentState = State.QUIT;
                        } else {
                            emailData.append(inputLine).append("\n");
                        }
                        break;

                    // === ÉTAT QUIT ===
                    case QUIT:
                        if (inputLine.equalsIgnoreCase("QUIT")) {
                            out.println("221 Bye");
                            socket.close();
                            return;
                        } else {
                            out.println("503 Bad sequence of commands");
                        }
                        break;

                    // === ÉTAT INCONNU ===
                    default:
                        out.println("500 Command not recognized");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
