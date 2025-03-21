package org.example.pop3;

import java.util.HashMap;
import java.util.Map;

public class UserAuthentication {
    private static final Map<String, String> users = new HashMap<>();

    static {
        users.put("DHIA", "123");
        users.put("MED", "mtps");
        users.put("POP", "POP");
    }

    public static boolean validateUser(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }
}
