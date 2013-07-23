package com.automattic.simplenote.utils;

/*
 *  misc. animation utilities
 *  added 01-Apr-2013 by Nick Bradbury
 */

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class AniUtils {
	private AniUtils() {
    	throw new AssertionError();
    }
	
	// fades in the passed view
	public final static void fadeIn(final View target) {
		fadeIn(target, null);
	}
    public final static void fadeIn(final View target, AnimationListener listener) {
    	if (target==null)
    		return;
    	
    	Animation animation = AnimationUtils.loadAnimation(target.getContext(), android.R.anim.fade_in);
    	if (listener!=null)
    		animation.setAnimationListener(listener);
        target.startAnimation(animation);

        if (target.getVisibility()!=View.VISIBLE)
    		target.setVisibility(View.VISIBLE);
    }
    
    // fades out the passed view
    public final static void fadeOut(final View target, int endVisibility) {
    	fadeOut(target, endVisibility, null);
    }
    public final static void fadeOut(final View target, int endVisibility, AnimationListener listener) {
    	if (target==null)
    		return;

    	Animation animation = AnimationUtils.loadAnimation(target.getContext(), android.R.anim.fade_out);
        if (listener!=null)
    		animation.setAnimationListener(listener);
        target.startAnimation(animation);

        if (target.getVisibility()!=endVisibility)
    		target.setVisibility(endVisibility);
    }
    
    // fade out the passed text view, then replace its text and fade it back in
    public final static void fadeTextOutIn(final TextView textView, final String newText) {
    	if (textView==null)
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
			public void onAnimationRepeat(Animation animation) { }
			@Override
			public void onAnimationStart(Animation animation) {	}
    	};
    	animationOut.setAnimationListener(outListener);
    	textView.startAnimation(animationOut);
    }
}