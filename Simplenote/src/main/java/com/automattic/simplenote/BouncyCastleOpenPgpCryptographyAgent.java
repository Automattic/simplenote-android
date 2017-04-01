package com.automattic.simplenote;

import com.automattic.simplenote.utils.PassphraseUtils;
import com.simperium.client.CryptographyAgent;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.spongycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.examples.ByteArrayHandler;

import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.Security;

// ATTRIBUTION:
// parts of this file were extracted from
// https://github.com/rtyley/spongycastle/blob/spongy-master/pg/src/main/java/org/spongycastle/openpgp/examples/ByteArrayHandler.java
// which uses this license from the original BouncyCastle:
//
// Except where otherwise stated, this software is distributed under a license
// based on the MIT X Consortium license. To view the license, see here:
// http://www.bouncycastle.org/licence.html
//
// Resources:
// https://www.codeproject.com/Articles/549119/Encryption-Wrapper-for-Android-SharedPreferences
// https://github.com/scottyab/secure-preferences
// https://github.com/tozny/java-aes-crypto

public class BouncyCastleOpenPgpCryptographyAgent extends CryptographyAgent {

    private static final String CIPHERTEXT_PROPERTY_NAME = "cipherText";

    public BouncyCastleOpenPgpCryptographyAgent() {
        Security.addProvider(new BouncyCastleProvider());
    }

    // Encrypts the rawObject into a string as property "cipherText" of a JSONObject.
    @Override
    public final JSONObject encryptJson(JSONObject rawObject) {
        String rawText = rawObject.toString();
        if (rawText == null || rawText.length() < 1) {
            return null;
        }
        try {
            JSONObject encryptedObject = new JSONObject();
            String cipherText;
            byte[] encryptedBytes = ByteArrayHandler.encrypt(
                    rawText.getBytes("UTF-8"),
                    PassphraseUtils.getPassphrase(),
                    "bcopca",
                    SymmetricKeyAlgorithmTags.AES_256,
                    true
            );
            cipherText = new String(encryptedBytes);
            encryptedObject.put(CIPHERTEXT_PROPERTY_NAME, cipherText);
            return encryptedObject;
        } catch (IOException | NoSuchProviderException | PGPException | JSONException e) {
            throw new RuntimeException("Exception encrypting text", e); // todo how to handle this?  We really want to stop syncing at this point and throw up an error screen. No possible recovery.
        }
    }

    // Decrypts the "cipherText" property of the JSONObject into a new JSONObject.
    @Override
    public final JSONObject decryptJson(JSONObject encryptedObject) {
        if (encryptedObject.has(CIPHERTEXT_PROPERTY_NAME)) {
            String cipherText = encryptedObject.optString(CIPHERTEXT_PROPERTY_NAME, "");
            if (cipherText == null || cipherText.length() < 1) {
                return new JSONObject(); // todo  is there a better way to handle this?
            }
            try {
                return new JSONObject(new JSONTokener(new String(ByteArrayHandler.decrypt(cipherText.getBytes("UTF-8"), PassphraseUtils.getPassphrase()))));
            } catch (IOException | NoSuchProviderException | PGPException | JSONException e) {
                throw new RuntimeException("Exception decrypting text ", e); // todo  how to handle this?  We really want to stop syncing at this point and throw up an error screen. No possible recovery.
            }
        }
        return encryptedObject;
    }
}