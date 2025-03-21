package org.example.pop3;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class POP3StateMachine {
    private POP3State state;
    private String user;
    private List<String> emails;
    private boolean authenticated;
    private boolean connectionActive;

    public POP3StateMachine() {
        this.state = POP3State.AUTHORIZATION;
        this.emails = new ArrayList<>();
        this.authenticated = false;
        this.connectionActive = true;
    }

    public String processCommand(String command) {
        if (!connectionActive) {
            handleDisconnection();
            return "-ERR Connection lost, restoring trash emails.";
        }

        String[] parts = command.split(" ");
        String cmd = parts[0].toUpperCase();

        switch (state) {
            case AUTHORIZATION:
                return handleAuthorization(cmd, parts);
            case TRANSACTION:
                return handleTransaction(cmd, parts);
            default:
                return "-ERR Unknown state";
        }
    }
    private void loadEmails() {
        Path userDir = Paths.get("mailserver", user);
        try {
            emails.clear();
            Files.list(userDir).forEach(file -> {
                if (!isDeleted(file)) {
                    emails.add(file.toString());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String handleAuthorization(String cmd, String[] parts) {
        switch (cmd) {
            case "USER":
                return handleUser(parts);
            case "PASS":
                return handlePass(parts);
            default:
                return "-ERR Authentication required";
        }
    }
    private String handleUser(String[] parts) {
        if (parts.length < 2) return "-ERR Missing username";
        this.user = parts[1];
        Path userDir = Paths.get("mailserver", user);
        if (Files.exists(userDir) && Files.isDirectory(userDir)) {
            return "+OK User accepted";
        } else {
            return "-ERR User not found";
        }
    }

    private String handlePass(String[] parts) {
        if (user == null) return "-ERR USER required first";
        if (parts.length < 2) return "-ERR Missing password";

        String password = parts[1];

        if (UserAuthentication.validateUser(user, password)) {
            authenticated = true;
            loadEmails();
            state = POP3State.TRANSACTION;
            return "+OK Password accepted";
        } else {
            return "-ERR Invalid password";
        }
    }

    private String handleTransaction(String cmd, String[] parts) {
        switch (cmd) {
            case "STAT":
                return handleStat();
            case "LIST":
                return handleList(parts);
            case "RETR":
                return handleRetr(parts);
            case "DELE":
                return handleDele(parts);
            case "RSET":
                return handleRset();
            case "NOOP":
                return "+OK";
            case "UIDL":
                return handleUidl(parts);
            case "TOP":
                return handleTop(parts);
            case "QUIT":
                return handleQuit();
            default:
                return "-ERR Command not allowed in this state";
        }
    }
    private String handleRetr(String[] parts) {
        if (parts.length < 2) return "-ERR Missing message number";
        int index = Integer.parseInt(parts[1]) - 1;
        if (index < 0 || index >= emails.size()) return "-ERR No such message";

        try {
            List<String> lines = Files.readAllLines(Paths.get(emails.get(index)));
            return "+OK " + lines.size() + " lines\n" + String.join("\r\n", lines) ;
        } catch (IOException e) {
            return "-ERR Error reading message";
        }
    }
    private String handleUidl(String[] parts) {
        if (parts.length == 2) {
            int index;
            try {
                index = Integer.parseInt(parts[1]) - 1;
            } catch (NumberFormatException e) {
                return "-ERR Invalid message number";
            }

            if (index < 0 || index >= emails.size()) {
                return "-ERR No such message";
            }

            return "+OK " + (index + 1) + " " + generateUID(emails.get(index)) + "\r\n";
        }

        StringBuilder response = new StringBuilder("+OK Unique IDs\r\n");
        for (int i = 0; i < emails.size(); i++) {
            response.append(i + 1).append(" ").append(generateUID(emails.get(i))).append("\r\n");
        }
        response.append(".\r\n");
        return response.toString();
    }

    private String handleTop(String[] parts) {
        if (parts.length < 3) return "-ERR Missing parameters";
        int index, numLines;
        try {
            index = Integer.parseInt(parts[1]) - 1;
            numLines = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return "-ERR Invalid parameters";
        }

        if (index < 0 || index >= emails.size()) return "-ERR No such message";

        try {
            List<String> lines = Files.readAllLines(Paths.get(emails.get(index)));
            StringBuilder response = new StringBuilder("+OK Header and first " + numLines + " lines\r\n");

            boolean inBody = false;
            int bodyLines = 0;
            for (String line : lines) {
                if (line.isEmpty()) {
                    inBody = true;
                    response.append("\r\n"); // Séparation entre en-tête et corps
                    continue;
                }
                if (!inBody) {
                    response.append(line).append("\r\n");
                } else if (bodyLines < numLines) {
                    response.append(line).append("\r\n");
                    bodyLines++;
                }
            }

            response.append(".\r\n");
            return response.toString();
        } catch (IOException e) {
            return "-ERR Error reading message";
        }
    }

    private String generateUID(String filename) {
        return Integer.toHexString(filename.hashCode());
    }

    private String handleStat() {
        long totalSize = emails.stream().mapToLong(file -> new File(file).length()).sum();
        return "+OK " + emails.size() + " " + totalSize;
    }
    private String handleList(String[] parts) {
        StringBuilder response = new StringBuilder("+OK " + emails.size() + " messages");
        for (int i = 0; i < emails.size(); i++) {
            response.append(".\r\n").append(i + 1).append(" ").append(new File(emails.get(i)).length());
        }
        response.append(".");
        return response.toString();
    }

    private String handleQuit() {
        deleteMarkedEmails();
        state = POP3State.AUTHORIZATION;
        return "+OK POP3 server signing off";
    }

    private String handleRset() {
        restoreDeletedEmails();
        loadEmails();
        return "+OK Reset - All deleted messages restored";
    }

    private String handleDele(String[] parts) {
        if (parts.length < 2) return "-ERR Missing message number";
        int index = Integer.parseInt(parts[1]) - 1;
        if (index >= 0 && index < emails.size()) {
            String emailPath = emails.get(index);
            moveToTrash(emailPath);
            emails.remove(index);
            return "+OK Message marked for deletion";
        } else {
            return "-ERR No such message";
        }
    }

    private void handleDisconnection() {
        System.out.println("⚠️ Connection lost. Restoring trash emails.");
        restoreDeletedEmails();
        deleteMarkedEmails();
    }

    private boolean isDeleted(Path emailPath) {
        return emailPath.toString().contains("trash");
    }

    private void moveToTrash(String emailPath) {
        Path originalPath = Paths.get(emailPath);
        Path trashPath = Paths.get(originalPath.getParent().toString(), "trash", originalPath.getFileName().toString());

        try {
            Files.createDirectories(trashPath.getParent());
            Files.move(originalPath, trashPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println(" Email moved to trash: " + trashPath);
        } catch (IOException e) {
            System.err.println("Error moving email to trash: " + e.getMessage());
        }
    }

    void restoreDeletedEmails() {
        Path trashDir = Paths.get("mailserver", user, "trash");
        Path inboxDir = Paths.get("mailserver", user);

        try {
            if (Files.exists(trashDir)) {
                Files.list(trashDir).forEach(file -> {
                    Path restoredPath = inboxDir.resolve(file.getFileName());
                    try {
                        Files.move(file, restoredPath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println(" Email restored: " + restoredPath);
                    } catch (IOException e) {
                        System.err.println("Error restoring email: " + e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            System.err.println(" Error accessing trash folder: " + e.getMessage());
        }
    }

    void deleteMarkedEmails() {
        Path trashDir = Paths.get("mailserver", user, "trash");

        try {
            if (Files.exists(trashDir)) {
                Files.list(trashDir).forEach(file -> {
                    try {
                        Files.delete(file);
                        System.out.println("🗑️ Email permanently deleted: " + file);
                    } catch (IOException e) {
                        System.err.println(" Error deleting email: " + e.getMessage());
                    }
                });

                Files.deleteIfExists(trashDir);
                System.out.println("🗑️ Trash folder deleted.");
            }
        } catch (IOException e) {
            System.err.println(" Error deleting trash folder: " + e.getMessage());
        }
    }
}
