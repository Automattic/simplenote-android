package com.automattic.simplenote;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.DimenRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.automattic.simplenote.utils.DrawableUtils;

/**
 * A {@link DialogFragment} implementing the full-screen dialog pattern defined in the
 * <a href="https://material.io/guidelines/components/dialogs.html#dialogs-full-screen-dialogs">
 * Material Design guidelines</a>.
 */
public class FullScreenDialogFragment extends DialogFragment {
    public static final String TAG = FullScreenDialogFragment.class.getSimpleName();

    private static final String ARG_ACTION = "ARG_ACTION";
    private static final String ARG_CONTAINER = "ARG_CONTAINER";
    private static final String ARG_ELEVATION = "ARG_ELEVATION";
    private static final String ARG_HIDE_ACTIVITY_BAR = "ARG_HIDE_ACTIVITY_BAR";
    private static final String ARG_SUBTITLE = "ARG_SUBTITLE";
    private static final String ARG_TITLE = "ARG_TITLE";
    private static final int ID_ACTION = 1;

    private Fragment mFragment;
    private FullScreenDialogController mController;
    private OnConfirmListener mOnConfirmListener;
    private OnDismissListener mOnDismissListener;
    private String mAction;
    private String mSubtitle;
    private String mTitle;
    private Toolbar mToolbar;
    private boolean mHideActivityBar;
    private float mElevation;
    @IdRes private int mContainer;

    public interface FullScreenDialogContent {
        boolean onConfirmClicked(FullScreenDialogController controller);
        boolean onDismissClicked(FullScreenDialogController controller);
        void onViewCreated(FullScreenDialogController controller);
    }

    public interface FullScreenDialogController {
        void confirm(@Nullable Bundle result);
        void dismiss();
    }

    public interface OnConfirmListener {
        void onConfirm(@Nullable Bundle result);
    }

    public interface OnDismissListener {
        void onDismiss();
    }

    protected static FullScreenDialogFragment newInstance(Builder builder) {
        FullScreenDialogFragment dialog = new FullScreenDialogFragment();
        dialog.setArguments(setArguments(builder));
        //noinspection deprecation
        dialog.setContent(Fragment.instantiate(builder.mContext, builder.mClass.getName(), builder.mArguments));
        dialog.setOnConfirmListener(builder.mOnConfirmListener);
        dialog.setOnDismissListener(builder.mOnDismissListener);
        dialog.setHideActivityBar(builder.mHideActivityBar);
        return dialog;
    }

    private static Bundle setArguments(Builder builder) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_ACTION, builder.mAction);
        bundle.putInt(ARG_CONTAINER, builder.mContainer);
        bundle.putFloat(ARG_ELEVATION, builder.mElevation);
        bundle.putString(ARG_TITLE, builder.mTitle);
        bundle.putString(ARG_SUBTITLE, builder.mSubtitle);
        bundle.putBoolean(ARG_HIDE_ACTIVITY_BAR, builder.mHideActivityBar);
        return bundle;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            getChildFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.full_screen_dialog_fragment_none,
                    0,
                    0,
                    R.anim.full_screen_dialog_fragment_none
                )
                .add(R.id.full_screen_dialog_fragment_content, mFragment)
                .commitNow();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mFragment = getChildFragmentManager().findFragmentById(R.id.full_screen_dialog_fragment_content);
        }

        mController = new FullScreenDialogController() {
            @Override
            public void confirm(@Nullable Bundle result) {
                FullScreenDialogFragment.this.confirm(result);
            }

            @Override
            public void dismiss() {
                FullScreenDialogFragment.this.dismiss();
            }
        };
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        initBuilderArguments();

        Dialog dialog = new Dialog(requireContext(), getTheme()) {
            @Override
            public void onBackPressed() {
                onDismissClicked();
            }
        };

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initBuilderArguments();

        if (mHideActivityBar) {
            hideActivityBar();
        }

        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_full_screen_dialog, container, false);
        initToolbar(view);
        setThemeBackground(view);
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ((FullScreenDialogContent) getContent()).onViewCreated(mController);
    }

    @Override
    public void dismiss() {
        // If isStateSaved() is true, it means that the application is not in the foreground, thus, we cannot
        // dismiss the dialog because it would cause an IllegalStateException
        if (isStateSaved()) {
            return;
        }

        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss();
        }

        if (mHideActivityBar) {
            showActivityBar();
        }

        super.dismiss();
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        show(manager.beginTransaction(), tag);
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        initBuilderArguments();
        transaction.setCustomAnimations(
            R.anim.full_screen_dialog_fragment_slide_up,
            0,
            0,
            R.anim.full_screen_dialog_fragment_slide_down
        );
        @IdRes int container = mContainer != 0 ? mContainer : android.R.id.content;
        return transaction.add(container, this, tag).addToBackStack(null).commit();
    }

    protected void confirm(Bundle result) {
        if (mOnConfirmListener != null) {
            mOnConfirmListener.onConfirm(result);
        }

        dismiss();
    }

    /**
     * Get {@link Fragment} to be able to interact directly with it.
     *
     * @return {@link Fragment} dialog content
     */
    public Fragment getContent() {
        return mFragment;
    }

    /**
     * Hide {@link androidx.appcompat.app.AppCompatActivity} bar when showing fullscreen dialog.
     */
    public void hideActivityBar() {
        FragmentActivity activity = getActivity();

        if (activity instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();

            if (actionBar != null && actionBar.isShowing()) {
                actionBar.hide();
            }
        }
    }

    /**
     * Initialize arguments passed in {@link Builder}.
     */
    private void initBuilderArguments() {
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            mAction = bundle.getString(ARG_ACTION);
            mContainer = bundle.getInt(ARG_CONTAINER);
            mElevation = bundle.getFloat(ARG_ELEVATION);
            mTitle = bundle.getString(ARG_TITLE);
            mSubtitle = bundle.getString(ARG_SUBTITLE);
            mHideActivityBar = bundle.getBoolean(ARG_HIDE_ACTIVITY_BAR);
        }
    }

    /**
     * Initialize toolbar title and action.
     *
     * @param view {@link View}
     */
    private void initToolbar(View view) {
        mToolbar = view.findViewById(R.id.full_screen_dialog_fragment_toolbar);
        mToolbar.setElevation(mElevation);
        mToolbar.setTitle(mTitle);
        mToolbar.setSubtitle(mSubtitle);
        mToolbar.setNavigationIcon(DrawableUtils.tintDrawableWithAttribute(view.getContext(), R.drawable.ic_cross_24dp, R.attr.toolbarIconColor));
        mToolbar.setNavigationContentDescription(R.string.description_close);
        mToolbar.setNavigationOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onDismissClicked();
                }
            }
        );

        if (!mAction.isEmpty()) {
            Menu menu = mToolbar.getMenu();
            MenuItem action = menu.add(0, ID_ACTION, 0, mAction);
            action.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            action.setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == ID_ACTION) {
                            onConfirmClicked();
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            );
        }
    }

    public void onBackPressed() {
        if (isAdded()) {
            onDismissClicked();
        }
    }

    protected void onConfirmClicked() {
        boolean isConsumed = ((FullScreenDialogContent) mFragment).onConfirmClicked(mController);

        if (!isConsumed) {
            mController.confirm(null);
        }
    }

    protected void onDismissClicked() {
        boolean isConsumed = ((FullScreenDialogContent) mFragment).onDismissClicked(mController);

        if (!isConsumed) {
            mController.dismiss();
        }
    }

    /**
     * Set {@link Fragment} as dialog content.
     *
     * @param fragment {@link Fragment} to set as dialog content
     */
    private void setContent(Fragment fragment) {
        mFragment = fragment;
    }

    /**
     * Set flag to hide activity bar when showing fullscreen dialog.
     *
     * @param hide boolean to hide activity bar
     */
    public void setHideActivityBar(boolean hide) {
        mHideActivityBar = hide;
    }

    /**
     * Set callback to call when dialog is closed due to confirm click.
     *
     * @param listener {@link OnConfirmListener} interface to call on confirm click
     */
    public void setOnConfirmListener(@Nullable OnConfirmListener listener) {
        mOnConfirmListener = listener;
    }

    /**
     * Set callback to call when dialog is closed due to dismiss click.
     *
     * @param listener {@link OnDismissListener} interface to call on dismiss click
     */
    public void setOnDismissListener(@Nullable OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    /**
     * Set {@link FullScreenDialogFragment} subtitle text.
     *
     * @param text {@link String} to set as subtitle text
     */
    public void setSubtitle(@NonNull String text) {
        mSubtitle = text;
        mToolbar.setSubtitle(mSubtitle);
    }

    /**
     * Set {@link FullScreenDialogFragment} subtitle text.
     *
     * @param textId resource ID to set as subtitle text
     */
    public void setSubtitle(@StringRes int textId) {
        mSubtitle = requireContext().getString(textId);
        mToolbar.setSubtitle(mSubtitle);
    }

    /**
     * Set theme background for {@link FullScreenDialogFragment} view.
     *
     * @param view {@link View} to set background
     */
    private void setThemeBackground(View view) {
        TypedValue value = new TypedValue();
        requireActivity().getTheme().resolveAttribute(android.R.attr.windowBackground, value, true);

        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            view.setBackgroundColor(value.data);
        } else {
            try {
                ViewCompat.setBackground(
                    view,
                    ResourcesCompat.getDrawable(requireActivity().getResources(), value.resourceId, requireActivity().getTheme())
                );
            } catch (Resources.NotFoundException ignore) {
            }
        }
    }

    /**
     * Show {@link androidx.appcompat.app.AppCompatActivity} bar when hiding fullscreen dialog.
     */
    public void showActivityBar() {
        FragmentActivity activity = getActivity();

        if (activity instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();

            if (actionBar != null && !actionBar.isShowing()) {
                actionBar.show();
            }
        }
    }

    public static class Builder {
        Bundle mArguments;
        Class<? extends Fragment> mClass;
        Context mContext;
        OnConfirmListener mOnConfirmListener;
        OnDismissListener mOnDismissListener;
        String mAction = "";
        String mSubtitle = "";
        String mTitle = "";
        boolean mHideActivityBar = false;
        float mElevation = 0;
        @IdRes int mContainer = 0;

        /**
         * Builder to construct {@link FullScreenDialogFragment}.
         *
         * @param context {@link Context}
         */
        public Builder(@NonNull Context context) {
            mContext = context;
            mElevation = mContext.getResources().getDimension(R.dimen.elevation_toolbar);
        }

        /**
         * Creates {@link FullScreenDialogFragment} with provided parameters.
         *
         * @return {@link FullScreenDialogFragment} instance created
         */
        public FullScreenDialogFragment build() {
            return FullScreenDialogFragment.newInstance(this);
        }

        /**
         * Set {@link FullScreenDialogFragment} action text.
         *
         * @param text {@link String} to set as action text
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setAction(@NonNull String text) {
            mAction = text;
            return this;
        }

        /**
         * Set {@link FullScreenDialogFragment} action text.
         *
         * @param textId resource ID to set as action text
         */
        public Builder setAction(@StringRes int textId) {
            return setAction(mContext.getString(textId));
        }

        /**
         * Set {@link Fragment} to be added as dialog, which must implement {@link FullScreenDialogContent}.
         *
         * @param contentClass     Fragment class to be instantiated
         * @param contentArguments arguments to be added to Fragment
         * @return {@link Builder} object to allow for chaining of calls to set methods
         * @throws IllegalArgumentException if content class does not implement
         *                                  {@link FullScreenDialogContent} interface
         */
        public Builder setContent(Class<? extends Fragment> contentClass, @Nullable Bundle contentArguments) {
            if (!FullScreenDialogContent.class.isAssignableFrom(contentClass)) {
                throw new IllegalArgumentException("The fragment class must implement FullScreenDialogContent interface");
            }

            mClass = contentClass;
            mArguments = contentArguments;
            return this;
        }

        /**
         * Set flag to hide activity bar when showing fullscreen dialog.
         *
         * @param hide boolean to hide activity bar
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setHideActivityBar(boolean hide) {
            mHideActivityBar = hide;
            return this;
        }

        /**
         * Set {@link FullScreenDialogFragment} subtitle text.
         *
         * @param text {@link String} to set as subtitle text
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setSubtitle(@NonNull String text) {
            mSubtitle = text;
            return this;
        }

        /**
         * Set {@link FullScreenDialogFragment} subtitle text.
         *
         * @param textId resource ID to set as subtitle text
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setSubtitle(@StringRes int textId) {
            mSubtitle = mContext.getString(textId);
            return this;
        }

        /**
         * Set {@link FullScreenDialogFragment} title text.
         *
         * @param text {@link String} to set as title text
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setTitle(@NonNull String text) {
            mTitle = text;
            return this;
        }

        /**
         * Set {@link FullScreenDialogFragment} title text.
         *
         * @param textId resource ID to set as title text
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setTitle(@StringRes int textId) {
            mTitle = mContext.getString(textId);
            return this;
        }

        /**
         * Set {@link FullScreenDialogFragment} toolbar elevation.
         *
         * @param elevation {@link Float} to set as toolbar elevation
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setToolbarElevation(float elevation) {
            mElevation = elevation;
            return this;
        }

        /**
         * Set {@link FullScreenDialogFragment} toolbar elevation.
         *
         * @param dimension resource ID dimension to set as toolbar elevation
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setToolbarElevation(@DimenRes int dimension) {
            if (dimension == 0) {
                return setToolbarElevation(Float.valueOf(dimension));
            } else {
                mElevation = mContext.getResources().getDimension(dimension);
                return this;
            }
        }

        /**
         * Set callback to call when dialog is closed due to confirm click.
         *
         * @param listener {@link OnConfirmListener} interface to call on confirm click
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setOnConfirmListener(@Nullable OnConfirmListener listener) {
            mOnConfirmListener = listener;
            return this;
        }

        /**
         * Set callback to call when dialog is closed due to dismiss click.
         *
         * @param listener {@link OnDismissListener} interface to call on dismiss click
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setOnDismissListener(@Nullable OnDismissListener listener) {
            mOnDismissListener = listener;
            return this;
        }

        /**
         * Set container view for full-screen dialog.
         *
         * @param container resource ID to use as container of full-screen dialog
         * @return {@link Builder} object to allow for chaining of calls to set methods
         */
        public Builder setViewContainer(@IdRes int container) {
            mContainer = container;
            return this;
        }
    }
}
