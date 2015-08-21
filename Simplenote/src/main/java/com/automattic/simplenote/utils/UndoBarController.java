/*
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.automattic.simplenote.utils;

import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.automattic.simplenote.R;

import java.util.List;

public class UndoBarController {
    private UndoListener mUndoListener;

    private List<String> mDeletedNoteIds;

    public interface UndoListener {
        void onUndo();
    }

    public UndoBarController(UndoListener undoListener) {
        mUndoListener = undoListener;
    }

    public void showUndoBar(View view, CharSequence message, Parcelable undoToken) {
        if (view == null) return;

        Snackbar
                .make(view, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, mOnUndoClickListener)
                .show();
    }

    private View.OnClickListener mOnUndoClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mUndoListener != null) {
                mUndoListener.onUndo();
            }
        }
    };

    public List<String> getDeletedNoteIds() {
        return mDeletedNoteIds;
    }

    public void setDeletedNoteIds(List noteIds) {
        mDeletedNoteIds = noteIds;
    }
}
