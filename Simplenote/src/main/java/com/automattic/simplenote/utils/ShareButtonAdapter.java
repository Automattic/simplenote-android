package com.automattic.simplenote.utils;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.automattic.simplenote.R;

import java.util.List;

/**
 * Created by Ondrej Ruttkay on 21/03/2016.
 */
public class ShareButtonAdapter extends RecyclerView.Adapter<ShareButtonAdapter.ViewHolder> {

    private List<ShareButtonItem> mItems;
    private ItemListener mListener;

    public ShareButtonAdapter(List<ShareButtonItem> items, ItemListener listener) {
        mItems = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.share_button_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setData(mItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView button;
        public ShareButtonItem item;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            button = (TextView) itemView.findViewById(R.id.share_button);
        }

        public void setData(ShareButtonItem item) {
            this.item = item;
            button.setCompoundDrawablesWithIntrinsicBounds(null, item.getDrawable(), null, null);
            button.setText(item.getTitle());
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onItemClick(item);
            }
        }
    }

    public interface ItemListener {
        void onItemClick(ShareButtonItem item);
    }

    public static class ShareButtonItem {

        private Drawable mDrawableRes;

        private CharSequence mTitle;
        private String mPackageName;
        private String mActivityName;

        public ShareButtonItem(Drawable drawable, CharSequence title,
                               String packageName, String activityName) {
            mDrawableRes = drawable;
            mTitle = title;
            mPackageName = packageName;
            mActivityName = activityName;
        }

        public Drawable getDrawable() {
            return mDrawableRes;
        }

        public CharSequence getTitle() {
            return mTitle;
        }

        public String getPackageName() {
            return mPackageName;
        }

        public String getActivityName() {
            return mActivityName;
        }
    }
}