package com.automattic.simplenote.utils;

import com.automattic.simplenote.BouncyCastleOpenPgpCryptographyAgent;
import com.automattic.simplenote.Simplenote;
import com.simperium.client.CryptographyAgent;

public class PassphraseUtils {

    public static char[] getPassphrase() {
        String encryptedStringPref = PrefUtils.getStringPref(Simplenote.getCryptoContext(), PrefUtils.PREF_CRYPTO_PASS_PHRASE, null);
        char[] passPhrase = encryptedStringPref == null ? null : encryptedStringPref.toCharArray();

        if (!CryptographyAgent.isEnabled()) { // in case the passPhrase is set _after_ the app has been started and the agent didn't get set yet.
            setCryptographyAgentInstance(passPhrase);
        }
        return passPhrase;
    }

    public static void setCryptographyAgentInstance() {
        setCryptographyAgentInstance(PassphraseUtils.getPassphrase());
    }

    private static void setCryptographyAgentInstance(char[] passPhrase) {
        CryptographyAgent cryptographyAgent = (passPhrase != null && passPhrase.length > 0) ? new BouncyCastleOpenPgpCryptographyAgent() : null;
        CryptographyAgent.setInstance(cryptographyAgent);
        // for testing
        // CryptographyAgent.setInstance(new Base64TestCryptographyAgent());
    }

}
