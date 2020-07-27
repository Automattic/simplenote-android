package com.automattic.simplenote.utils;

import android.util.Log;

import com.automattic.simplenote.models.Tag;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.BucketObjectNameInvalid;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

public class TagUtils {
    private static int MAXIMUM_LENGTH_ENCODED_HASH = 256;

    /**
     * Create a tag with the @param key and @param name in the @param bucket.
     *
     * @param bucket    {@link Bucket<Tag>} in which to create the tag.
     * @param name      {@link String} to use as tag name.
     * @param index     {@link int} to use as tag index.
     */
    public static void createTag(Bucket<Tag> bucket, String name, int index) throws BucketObjectNameInvalid {
        try {
            Tag tag = bucket.newObject(hashTag(name));
            tag.setName(name);
            tag.setIndex(index);
            tag.save();
        } catch (BucketObjectNameInvalid e) {
            Log.e("createTag", "Could not create tag " + "\"" + name + "\"", e);
            throw new BucketObjectNameInvalid(name);
        }
    }

    /**
     * Create a tag the @param key and @param name in the @param bucket if it does not exist.
     *
     * @param bucket    {@link Bucket<Tag>} in which to create the tag.
     * @param name      {@link String} to use creating the tag.
     */
    public static void createTagIfMissing(Bucket<Tag> bucket, String name) throws BucketObjectNameInvalid {
        try {
            bucket.getObject(hashTag(name));
            Log.d("createTagIfMissing", "Tag " + "\"" + name + "\"" + " already exists");
        } catch (BucketObjectMissingException e) {
            createTag(bucket, name, bucket.count());
            Log.d("createTagIfMissing", "Tag " + "\"" + name + "\"" + " does not exist");
        }
    }

    /**
     * Hash the tag @param name with normalizing, lowercasing, and encoding.
     *
     * @param name      {@link String} to hash as the tag kay.
     *
     * @return          {@link String} hashed to use as tag key.
     */
    public static String hashTag(String name) {
        try {
            String normalized = Normalizer.normalize(name, Normalizer.Form.NFC);
            String lowercased = normalized.toLowerCase(Locale.US);
            String encoded = URLEncoder.encode(lowercased, StandardCharsets.UTF_8.toString());
            return encoded.replace("*", "%2A").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            // TODO: Handle encoding exception with a custom UTF-8 encoder.
            return name;
        }
    }

    /**
     * Determine if the hashed tag @param name is valid after normalizing, lowercasing, and encoding.
     *
     * @param name      {@link String} to hash as the tag kay.
     *
     * @return          {@link boolean} true if hashed value is valid; false otherwise.
     */
    public static boolean hashTagValid(String name) {
        try {
            String normalized = Normalizer.normalize(name, Normalizer.Form.NFC);
            String lowercased = normalized.toLowerCase(Locale.US);
            String encoded = URLEncoder.encode(lowercased, StandardCharsets.UTF_8.toString()).replace("*", "%2A").replace("+", "%20");
            return encoded.length() <= MAXIMUM_LENGTH_ENCODED_HASH;
        } catch (UnsupportedEncodingException e) {
            // TODO: Handle encoding exception with a custom UTF-8 encoder.
            return false;
        }
    }
}
