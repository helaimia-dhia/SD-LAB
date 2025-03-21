package org.example.pop3;

public enum POP3State {
    AUTHORIZATION, // État initial (authentification)
    TRANSACTION,   // Consultation des emails
    UPDATE         // Suppression et déconnexion
}

