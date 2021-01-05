package com.automattic.simplenote.utils;

import android.animation.Animator;
import android.util.Property;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateInterpolator;
import android.widget.TextView;

@SuppressWarnings("unused")
public class AniUtils {
    private AniUtils() {
        throw new AssertionError();
    }

    // fades in the passed view
    public static void fadeIn(final View target) {
        fadeIn(target, null);
    }

    public static void fadeIn(final View target, AnimationListener listener) {
        if (target == null)
            return;

        Animation animation = AnimationUtils.loadAnimation(target.getContext(), android.R.anim.fade_in);
        if (listener != null)
            animation.setAnimationListener(listener);
        target.startAnimation(animation);

        if (target.getVisibility() != View.VISIBLE)
            target.setVisibility(View.VISIBLE);
    }

    // fades out the passed view
    public static void fadeOut(final View target, int endVisibility) {
        fadeOut(target, endVisibility, null);
    }

    public static void fadeOut(final View target, int endVisibility, AnimationListener listener) {
        if (target == null)
            return;

        Animation animation = AnimationUtils.loadAnimation(target.getContext(), android.R.anim.fade_out);
        if (listener != null)
            animation.setAnimationListener(listener);
        target.startAnimation(animation);

        if (target.getVisibility() != endVisibility)
            target.setVisibility(endVisibility);
    }

    // fade out the passed text view, then replace its text and fade it back in
    public static void fadeTextOutIn(final TextView textView, final String newText) {
        if (textView == null)
            return;

        Animation animationOut = AnimationUtils.loadAnimation(textView.getContext(), android.R.anim.fade_out);
        AnimationListener outListener = new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                Animation animationIn = AnimationUtils.loadAnimation(textView.getContext(), android.R.anim.fade_in);
                textView.setText(newText);
                textView.startAnimation(animationIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        };
        animationOut.setAnimationListener(outListener);
        textView.startAnimation(animationOut);
    }

    // Animates the view off-screen to the left
    public static void swipeOutToLeft(final View view) {
        if (view == null) return;

        view.animate()
                .xBy(-view.getWidth())
                .alpha(0.0f)
                .setInterpolator(new AnticipateInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                })
                .start();
    }

    /**
     * An implementation of {@link android.util.Property} to be used specifically with fields of
     * type <code>float</code>. This type-specific subclass enables performance benefit by allowing
     * calls to a {@link #set(Object, Float) set()} function that takes the primitive
     * <code>float</code> type and avoids autoboxing and other overhead associated with the
     * <code>Float</code> class.
     *
     * @param <T> The class on which the Property is declared.
     **/
    public static abstract class FloatProperty<T> extends Property<T, Float> {
        public FloatProperty(String name) {
            super(Float.class, name);
        }

        /**
         * A type-specific override of the {@link #set(Object, Float)} that is faster when dealing
         * with fields of type <code>float</code>.
         */
        public abstract void setValue(T object, float value);

        @Override
        final public void set(T object, Float value) {
            setValue(object, value);
        }
    }

    /**
     * An implementation of {@link android.util.Property} to be used specifically with fields of
     * type <code>int</code>. This type-specific subclass enables performance benefit by allowing
     * calls to a {@link #set(Object, Integer) set()} function that takes the primitive
     * <code>int</code> type and avoids autoboxing and other overhead associated with the
     * <code>Integer</code> class.
     *
     * @param <T> The class on which the Property is declared.
     */
    public static abstract class IntProperty<T> extends Property<T, Integer> {
        public IntProperty(String name) {
            super(Integer.class, name);
        }

        /**
         * A type-specific override of the {@link #set(Object, Integer)} that is faster when dealing
         * with fields of type <code>int</code>.
         */
        public abstract void setValue(T object, int value);

        @Override
        final public void set(T object, Integer value) {
            setValue(object, value);
        }
    }
}
