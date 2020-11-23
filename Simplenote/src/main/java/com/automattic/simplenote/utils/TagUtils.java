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
        if (isTagMissing(bucket, name)) {
            createTag(bucket, name, bucket.count());
        }
    }

    /**
     * Get the canonical representation of a tag from the hashed value of the lexical variation.
     *
     * @param bucket    {@link Bucket<Tag>} in which to get tag.
     * @param lexical   {@link String} lexical variation of tag.
     *
     * @return          {@link String} canonical of tag if exists; lexical variation otherwise.
     */
    public static String getCanonicalFromLexical(Bucket<Tag> bucket, String lexical) {
        String hashed = hashTag(lexical);

        try {
            Tag tag = bucket.getObject(hashed);
            Log.d("getCanonicalFromLexical", "Tag " + "\"" + hashed + "\"" + " does exist");
            return tag.getName();
        } catch (BucketObjectMissingException e) {
            Log.d("getCanonicalFromLexical", "Tag " + "\"" + hashed + "\"" + " does not exist");
            return lexical;
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
            String encoded = URLEncoder.encode(lowercased, StandardCharsets.UTF_8.name());
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
            String encoded = URLEncoder.encode(lowercased, StandardCharsets.UTF_8.name()).replace("*", "%2A").replace("+", "%20");
            return encoded.length() <= MAXIMUM_LENGTH_ENCODED_HASH;
        } catch (UnsupportedEncodingException e) {
            // TODO: Handle encoding exception with a custom UTF-8 encoder.
            return false;
        }
    }

    /**
     * Determine if the tag with @param name is missing from @param bucket or not.
     *
     * @param bucket    {@link Bucket<Tag>} in which to create the tag.
     * @param name      {@link String} to use creating the tag.
     *
     * @return          {@link Boolean} true if tag is missing; false otherwise.
     */
    public static boolean isTagMissing(Bucket<Tag> bucket, String name) {
        try {
            bucket.getObject(hashTag(name));
            Log.d("createTagIfMissing", "Tag " + "\"" + name + "\"" + " already exists");
            return false;
        } catch (BucketObjectMissingException e) {
            Log.d("createTagIfMissing", "Tag " + "\"" + name + "\"" + " does not exist");
            return true;
        }
    }
}
