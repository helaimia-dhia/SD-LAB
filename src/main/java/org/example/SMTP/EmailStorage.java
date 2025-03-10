package org.example.SMTP;

import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EmailStorage {
    private static final String MAIL_DIR = "mailserver/";

    public static void storeEmail(String user, String message) throws IOException {
        String userDir = MAIL_DIR + user;
        Files.createDirectories(Paths.get(userDir));
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String filePath = userDir + "/" + timestamp + ".txt";
        Files.write(Paths.get(filePath), message.getBytes());
        System.out.println("Email stocké dans : " + filePath);
    }
}
