package org.example.smtp;

import java.util.ArrayList;
import java.util.List;

public class SMTPStateMachine {
    private SMTPState currentState;
    private String sender;
    private List<String> recipients;
    private StringBuilder emailContent;
    private String domain; // Stocke le domaine après HELO

    public SMTPStateMachine() {
        this.currentState = SMTPState.INIT;
        this.emailContent = new StringBuilder();
        this.recipients = new ArrayList<>();
        this.domain = null;
    }

    public String processCommand(String command) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0];

        switch (currentState) {
            case INIT:
                if (cmd.equals("HELO") || cmd.equals("EHLO")) {
                    if (parts.length < 2) return "501 Syntax error: Missing domain";

                    domain = parts[1].trim(); // Stocke le nom de domaine
                    currentState = SMTPState.HELO;
                    return "250 " + domain;
                } else if (cmd.equals("QUIT")) {
                    currentState = SMTPState.QUIT;
                    return "221 Bye";
                }else if (!cmd.equals("HELO") || !cmd.equals("EHLO")){
                    return " 503 Bad sequence of commands";
                }
                else {
                    return "501 Syntax error in parameters or arguments";
                }

            case HELO:
                if (cmd.equals("MAIL") && parts.length > 1 && parts[1].startsWith("FROM:")) {
                    sender = parts[1].substring(5).trim();

                    if (!isValidEmail(sender)) {
                        return "501 Invalid email address";
                    }

                    currentState = SMTPState.MAIL;
                    return "250 OK";
                } else if (cmd.equals("QUIT")) {
                    currentState = SMTPState.QUIT;
                    return "221 Bye";
                } else if (cmd.equals("NOOP")) {
                    return "250 OK";
                } else if (cmd.equals("RSET")) {
                    resetTransaction();
                    return "250 OK - Transaction reset";
                } else if (! (cmd.equals("MAIL") && parts.length > 1 && parts[1].startsWith("FROM:"))) {
                    return " 503 Bad sequence of commands";
                } else {
                    return "501 Syntax error in parameters or arguments";
                }

            case MAIL:
            case RCPT:
                if (cmd.equals("RCPT") && parts.length > 1 && parts[1].startsWith("TO:")) {
                    String recipient = parts[1].substring(3).trim();

                    if (!isValidEmail(recipient)) {
                        return "501 Invalid email address";
                    }

                    recipients.add(recipient);
                    currentState = SMTPState.RCPT;
                    return "250 OK";
                } else if (cmd.equals("DATA")) {
                    if (recipients.isEmpty()) {
                        return "503 No recipient specified";
                    }
                    currentState = SMTPState.DATA;
                    return "354 Start mail input; end with <CRLF>.<CRLF>";
                } else if (cmd.equals("QUIT")) {
                    currentState = SMTPState.QUIT;
                    return "221 Bye";
                } else if (cmd.equals("NOOP")) {
                    return "250 OK";
                } else if (cmd.equals("RSET")) {
                    resetTransaction();
                    return "250 OK - Transaction reset";
                } else {
                    return "501 Syntax error in parameters or arguments";
                }

            case DATA:

                if (command.equals(".")) {
                    for (String recipient : recipients) {
                        String recipientUsername = extractUsername(recipient);
                        if (recipientUsername != null) {
                            EmailStorage.storeEmail(recipientUsername, recipient, sender, emailContent.toString());
                            System.out.println("Email saved for: " + recipientUsername);
                        } else {
                            System.err.println(" Invalid recipient format: " + recipient);
                        }
                    }

                    resetTransaction();
                    return "250 OK";
                } else if (command.equals("QUIT")) {
                    currentState = SMTPState.QUIT;
                    return "221 Bye";
                } else {
                    emailContent.append(command).append("\n");
                    return null;
                }

            case QUIT:
                if (cmd.equals("QUIT")) {
                    return "221 Bye";
                } else {
                    return "503 Bad sequence of commands";
                }

            default:
                return "500 Command not recognized";
        }
    }

    public SMTPState getCurrentState() {
        return currentState;
    }

    private void resetTransaction() {
        sender = null;
        recipients.clear();
        emailContent.setLength(0);
        currentState = SMTPState.HELO;
    }

    private String extractUsername(String email) {
        if (email != null && email.contains("@")) {
            return email.split("@")[0].trim();
        }
        return null;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.toLowerCase().endsWith(".com");
    }
}
