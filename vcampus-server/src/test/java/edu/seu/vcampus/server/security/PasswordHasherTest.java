package edu.seu.vcampus.server.security;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PasswordHasherTest {
    @Test
    public void hashCanBeVerifiedWithoutStoringPlaintext() {
        PasswordHasher hasher = new PasswordHasher(4);
        String hash = hasher.hash("secret");

        assertNotEquals("secret", hash);
        assertTrue(hasher.matches("secret", hash));
        assertFalse(hasher.matches("wrong", hash));
        assertFalse(hasher.matches("secret", "not-a-bcrypt-hash"));
    }
}
