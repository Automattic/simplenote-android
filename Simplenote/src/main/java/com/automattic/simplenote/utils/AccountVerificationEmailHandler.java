package com.automattic.simplenote.utils;

import androidx.annotation.NonNull;

public interface AccountVerificationEmailHandler {
    void onSuccess(@NonNull String url);
    void onFailure(@NonNull Exception e, @NonNull String url);
}
