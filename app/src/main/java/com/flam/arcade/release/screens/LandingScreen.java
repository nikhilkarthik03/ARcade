package com.flam.arcade.release.screens;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import com.flam.arcade.R;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class LandingScreen {

    public LandingScreen(Context context, ArFragment arFragment) {
        FrameLayout landingScreen = ((Activity) context).findViewById(R.id.LandingScreen);
        FrameLayout instructionsView = ((Activity) context).findViewById(R.id.instructionsView);
        GifImageView gifImageView = ((Activity) context).findViewById(R.id.logo);

        try {
            GifDrawable gifDrawable = new GifDrawable(context.getResources(), R.raw.logo);
            gifDrawable.setLoopCount(1); // Play only once
            gifImageView.setImageDrawable(gifDrawable);
            gifDrawable.start(); // Ensure the animation starts

            gifDrawable.addAnimationListener(loopNumber -> {
                if (loopNumber == 0) {
                    // Remove landing screen
                    if (landingScreen != null && landingScreen.getParent() instanceof FrameLayout) {
                        ((FrameLayout) landingScreen.getParent()).removeView(landingScreen);
                    }

                    // Show instructions screen
                    if (instructionsView != null) {
                        instructionsView.setVisibility(View.VISIBLE);
                        new Instructions(context, arFragment);
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
