package com.automattic.simplenote.smoke.data;

import java.util.Random;

public class SignUpDataProvider {

    private static final int MAX_EMAIL_NAME_LENGTH = 9;
    private static final int MIN_EMAIL_NAME_LENGTH = 2;
    private static final int MAX_EMAIL_DOMAIN_LENGTH = 5;
    private static final int MIN_EMAIL_DOMAIN_LENGTH = 2;
    private static final int MAX_EMAIL_TOP_LEVEL_LENGTH = 2;
    private static final int MIN_EMAIL_TOP_LEVEL_LENGTH = 2;
    private static final int PASSWORD_LENGTH = 8;
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final Random random = new Random();

    /**
     * Generates an email address with random lengths
     *
     * @return the email address
     */
    public static String generateEmail() {
        int nameLength;
        int domainLength;
        int topLevelLength;

        nameLength = random.nextInt(MAX_EMAIL_NAME_LENGTH) + MIN_EMAIL_NAME_LENGTH;
        domainLength = random.nextInt(MAX_EMAIL_DOMAIN_LENGTH) + MIN_EMAIL_DOMAIN_LENGTH;
        topLevelLength = random.nextInt(MAX_EMAIL_TOP_LEVEL_LENGTH) + MIN_EMAIL_TOP_LEVEL_LENGTH;

        return generateEmail(nameLength, domainLength, topLevelLength);
    }

    public static String generatePassword() {
        return generateString(PASSWORD_LENGTH);
    }

    /**
     * Generates random email with the length of the required parameters
     *
     * @param nameLength     length of the first part of email (4 "abcd"@email.com)
     * @param domainLength   length of the domain part of email (5 abcd@"email".com)
     * @param topLevelLength length of the top level part of email (3 abcd@email."com")
     * @return generated email
     */
    private static String generateEmail(int nameLength, int domainLength, int topLevelLength) {
        StringBuilder builder = new StringBuilder();
        builder.append(generateString(nameLength));
        builder.append("@");
        builder.append(generateString(domainLength));
        builder.append(".");
        builder.append(generateString(topLevelLength));
        return builder.toString();
    }

    /**
     * Generates random string values with the given length
     *
     * @param length Length of the string to be generated.
     * @return Randomly geneated string.
     */
    private static String generateString(int length) {
        StringBuilder generated = new StringBuilder();
        while (generated.length() < length) { // length of the random string.
            int index = (int) (random.nextFloat() * CHARACTERS.length());
            generated.append(CHARACTERS.charAt(index));
        }
        return generated.toString();
    }
}