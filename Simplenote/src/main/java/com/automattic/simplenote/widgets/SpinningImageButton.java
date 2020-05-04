package com.automattic.simplenote.widgets;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.widget.AppCompatImageButton;

@SuppressWarnings("unused")
public class SpinningImageButton extends AppCompatImageButton {
    private static final long LENGTH_ACCELERATE = 750;
    private static final long LENGTH_DECELERATE = 600;
    private static final long LENGTH_FULL_SPEED = 250;
    private static final long LENGTH_LONG_PRESS = 1000;

    private ObjectAnimator mAnimator;
    private SpeedListener mListener;
    private boolean mIsMaximumSpeed;
    private float mSlop;

    private final Runnable mLongPressCallback = new Runnable() {
        @Override
        public void run() {
            startAccelerationSpin();
        }
    };

    public interface SpeedListener {
        void OnMaximumSpeed();
    }

    public SpinningImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                postDelayed(mLongPressCallback, LENGTH_LONG_PRESS);
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelLongClick();
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();

                if ((x < -mSlop) || (y < -mSlop) || (x > getWidth() + mSlop) || (y > getHeight() + mSlop)) {
                    cancelLongClick();
                }

                break;
            case MotionEvent.ACTION_UP:
                if (mIsMaximumSpeed) {
                    startExitAnimation();
                } else {
                    cancelLongClick();
                }

                break;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        super.setBackgroundDrawable(null);
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        super.setScaleType(ScaleType.FIT_CENTER);
    }

    public boolean isAnimating() {
        return mAnimator != null && mAnimator.isRunning();
    }

    public boolean isMaximumSpeed() {
        return mIsMaximumSpeed;
    }

    private void cancelAnimation() {
        if (mAnimator != null) {
            mAnimator.removeAllListeners();
            mAnimator.cancel();
            mAnimator = null;
        }
    }

    private void cancelLongClick() {
        cancelAnimation();
        mIsMaximumSpeed = false;
        removeCallbacks(mLongPressCallback);
        setRotation(0);
    }

    public void setSpeedListener(SpeedListener listener) {
        mListener = listener;
    }

    protected void startAccelerationSpin() {
        cancelAnimation();
        mAnimator = ObjectAnimator.ofFloat(this, View.ROTATION, 360, 0);
        mAnimator.setInterpolator(
            AnimationUtils.loadInterpolator(getContext(),
            android.R.interpolator.accelerate_quad)
        );
        mAnimator.setDuration(LENGTH_ACCELERATE);
        mAnimator.addListener(
            new AnimatorListener() {
                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    startContinuousSpin();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationStart(Animator animation) {
                }
            }
        );
        mAnimator.start();
    }

    protected void startContinuousSpin() {
        if (mListener != null) {
            mListener.OnMaximumSpeed();
        }

        cancelAnimation();
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        mIsMaximumSpeed = true;
        mAnimator = ObjectAnimator.ofFloat(this, View.ROTATION, 360, 0);
        mAnimator.setInterpolator(
            AnimationUtils.loadInterpolator(getContext(),
            android.R.interpolator.linear)
        );
        mAnimator.setDuration(LENGTH_FULL_SPEED);
        mAnimator.setRepeatCount(Animation.INFINITE);
        mAnimator.start();
    }

    private void startExitAnimation() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, View.ROTATION, 360, 0);
        animator.setDuration(LENGTH_DECELERATE);
        animator.setInterpolator(
            AnimationUtils.loadInterpolator(getContext(),
            android.R.interpolator.decelerate_cubic)
        );
        animator.addListener(
            new AnimatorListener() {
                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    cancelAnimation();
                    cancelLongClick();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationStart(Animator animation) {
                }
            }
        );
        animator.start();
    }
}
