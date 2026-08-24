package org.apache.commons.compress.archivers.sevenz;

import java.security.SecureRandom;
import java.util.Arrays;

/** Android-compatible construction of Commons Compress 7Z AES encoder options. */
public final class AndroidSevenZEncryption {

    private static final int AES_IV_BYTES = 16;
    private static final int PASSWORD_CYCLES_POWER = 19;
    private static final byte[] EMPTY_SALT = new byte[0];

    private AndroidSevenZEncryption() {
    }

    /**
     * Configures the same AES-256/SHA-256 coder chain as the password constructor without calling
     * {@code SecureRandom.getInstanceStrong()}, which is unavailable before Android 8.
     */
    public static void configure(
            SevenZOutputFile output,
            char[] password,
            SevenZMethodConfiguration compression
    ) {
        if (output == null || password == null || password.length == 0 || compression == null) {
            throw new IllegalArgumentException("7Z encryption arguments must be present");
        }
        byte[] iv = new byte[AES_IV_BYTES];
        new SecureRandom().nextBytes(iv);
        AES256Options encryptionOptions = new AES256Options(
                password,
                EMPTY_SALT,
                iv,
                PASSWORD_CYCLES_POWER
        );
        SevenZMethodConfiguration encryption = new SevenZMethodConfiguration(
                SevenZMethod.AES256SHA256,
                encryptionOptions
        );

        // SevenZOutputFile reverses this iterable internally. Compression must feed encryption.
        output.setContentMethods(Arrays.asList(compression, encryption));
    }
}
