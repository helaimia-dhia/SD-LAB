package org.example.smtp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EmailStorage {
    public static void storeEmail(String username, String recipient, String sender, String emailContent) {
        // Définir le répertoire du destinataire
        String directory = "mailserver/" + username + "/";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = directory + timestamp + ".txt";

        try {
            // Vérifier et créer le répertoire si nécessaire
            Files.createDirectories(Paths.get(directory));

            // Écriture de l'email dans le fichier
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write("From: " + sender + "\n");
                writer.write("To: " + recipient + "\n\n"); // Séparation en-tête/contenu
                writer.write(emailContent);
            }

            System.out.println("✅ Email stored successfully in: " + filename);
        } catch (IOException e) {
            System.err.println("❌ Error storing email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Déplacer un email dans le dossier trash/ (marquer comme supprimé)
    public static boolean deleteEmail(String username, String filename) {
        Path originalPath = Paths.get("mailserver/" + username + "/" + filename);
        Path trashPath = Paths.get("mailserver/" + username + "/trash/" + filename);

        try {
            // Créer le dossier trash si nécessaire
            Files.createDirectories(trashPath.getParent());

            // Déplacer l'email vers trash
            Files.move(originalPath, trashPath);
            System.out.println("🗑️ Email moved to trash: " + filename);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Error moving email to trash: " + e.getMessage());
            return false;
        }
    }

    // Restaurer tous les emails du dossier trash/
    public static void resetEmails(String username) {
        Path trashDir = Paths.get("mailserver/" + username + "/trash/");
        Path inboxDir = Paths.get("mailserver/" + username + "/");

        try {
            if (Files.exists(trashDir)) {
                Files.list(trashDir).forEach(file -> {
                    Path restoredPath = inboxDir.resolve(file.getFileName());
                    try {
                        Files.move(file, restoredPath);
                        System.out.println("♻️ Email restored: " + file.getFileName());
                    } catch (IOException e) {
                        System.err.println("❌ Error restoring email: " + e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("❌ Error accessing trash folder: " + e.getMessage());
        }
    }

    // Vérifier si un email est dans la corbeille (pour éviter de l'afficher)
    public static boolean isEmailDeleted(String username, String filename) {
        Path trashPath = Paths.get("mailserver/" + username + "/trash/" + filename);
        return Files.exists(trashPath);
    }
}
