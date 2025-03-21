package org.example.smtp;

public enum SMTPState {
    INIT,       // État initial
    HELO,       // Après la commande HELO/EHLO
    MAIL,       // Après la commande MAIL FROM
    RCPT,       // Après la commande RCPT TO
    DATA, // Après la commande DATA
    QUIT        // Après la commande QUIT
}

