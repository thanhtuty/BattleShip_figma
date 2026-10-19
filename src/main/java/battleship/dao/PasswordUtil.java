package battleship.dao;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

public final class PasswordUtil {
    private PasswordUtil() {}

    public static String newSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    public static String hash(String password, String saltHex) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(HexFormat.of().parseHex(saltHex));
            return HexFormat.of().formatHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
