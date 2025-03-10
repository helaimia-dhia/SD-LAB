package org.example.POP3;



import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class POP3ClientHandler {
    private Socket socket;
    private enum State { AUTH, TRANSACTION, UPDATE }
    private State currentState = State.AUTH;
    private String username = null;
    private String userDirectory = null;

    public POP3ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Réponse de bienvenue
            out.println("+OK POP3 Server ready");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received: " + inputLine);

                // === Gérer QUIT dans tous les états ===
                if (inputLine.equalsIgnoreCase("QUIT")) {
                    out.println("+OK Goodbye");
                    socket.close();
                    return;
                }

                switch (currentState) {
                    // === ÉTAT AUTH ===
                    case AUTH:
                        if (inputLine.startsWith("USER ")) {
                            username = inputLine.substring(5).trim();
                            userDirectory = "mailserver/" + username;
                            File dir = new File(userDirectory);
                            if (dir.exists() && dir.isDirectory()) {
                                out.println("+OK User accepted");
                            } else {
                                out.println("-ERR No such user");
                            }
                        } else if (inputLine.startsWith("PASS ")) {
                            if (username != null) {
                                out.println("+OK Password accepted");
                                currentState = State.TRANSACTION;  // Passer à l'état TRANSACTION
                            } else {
                                out.println("-ERR USER command required");
                            }
                        } else {
                            out.println("-ERR Invalid command in AUTH state");
                        }
                        break;

                    // === ÉTAT TRANSACTION ===
                    case TRANSACTION:
                        if (inputLine.equalsIgnoreCase("STAT")) {
                            File dir = new File(userDirectory);
                            File[] emails = dir.listFiles();
                            int count = emails != null ? emails.length : 0;
                            long size = 0;
                            if (emails != null) {
                                for (File email : emails) {
                                    size += email.length();
                                }
                            }
                            out.println("+OK " + count + " " + size);
                        } else if (inputLine.equalsIgnoreCase("LIST")) {
                            File dir = new File(userDirectory);
                            File[] emails = dir.listFiles();
                            if (emails != null && emails.length > 0) {
                                out.println("+OK Listing messages");
                                for (int i = 0; i < emails.length; i++) {
                                    out.println((i + 1) + " " + emails[i].length());
                                }
                                out.println(".");
                            } else {
                                out.println("-ERR No messages");
                            }
                        } else if (inputLine.startsWith("RETR ")) {
                            int messageId = Integer.parseInt(inputLine.substring(5).trim()) - 1;
                            File dir = new File(userDirectory);
                            File[] emails = dir.listFiles();
                            if (emails != null && messageId >= 0 && messageId < emails.length) {
                                out.println("+OK Message follows");
                                BufferedReader emailReader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(emails[messageId])));
                                String line;
                                while ((line = emailReader.readLine()) != null) {
                                    out.println(line);
                                }
                                out.println(".");
                                emailReader.close();
                            } else {
                                out.println("-ERR No such message");
                            }
                        } else if (inputLine.equalsIgnoreCase("NOOP")) {
                            out.println("+OK");
                        } else if (inputLine.equalsIgnoreCase("RSET")) {
                            out.println("+OK Reset state");
                        } else if (inputLine.startsWith("DELE ")) {
                            out.println("+OK Message marked for deletion");  // Simplicité : ne supprime pas vraiment
                        } else {
                            out.println("-ERR Invalid command in TRANSACTION state");
                        }
                        break;

                    // === ÉTAT UPDATE (après QUIT) ===
                    case UPDATE:
                        out.println("-ERR Invalid command in UPDATE state");
                        break;

                    // === ÉTAT INCONNU ===
                    default:
                        out.println("-ERR Command not recognized");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
