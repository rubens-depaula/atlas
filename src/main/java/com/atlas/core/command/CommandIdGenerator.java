package com.atlas.core.command;

import com.atlas.command.CommandId;

import java.math.BigInteger;
import java.security.SecureRandom;

public final class CommandIdGenerator {

    private static final String ALPHABET =
            "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    private static final BigInteger MASK =
            BigInteger.valueOf(31);

    private final SecureRandom random =
            new SecureRandom();

    public CommandId next() {
        byte[] bytes = new byte[16];

        long timestamp =
                System.currentTimeMillis();

        bytes[0] = (byte) (timestamp >>> 40);
        bytes[1] = (byte) (timestamp >>> 32);
        bytes[2] = (byte) (timestamp >>> 24);
        bytes[3] = (byte) (timestamp >>> 16);
        bytes[4] = (byte) (timestamp >>> 8);
        bytes[5] = (byte) timestamp;

        byte[] randomness = new byte[10];
        random.nextBytes(randomness);

        System.arraycopy(
                randomness,
                0,
                bytes,
                6,
                randomness.length
        );

        BigInteger value =
                new BigInteger(1, bytes);

        StringBuilder ulid =
                new StringBuilder(26);

        for (int i = 0; i < 26; i++) {
            int index =
                    value.and(MASK).intValue();

            ulid.append(
                    ALPHABET.charAt(index)
            );

            value = value.shiftRight(5);
        }

        return new CommandId(
                "cmd_" + ulid.reverse()
        );
    }
}