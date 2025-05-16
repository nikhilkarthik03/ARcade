package com.flam.arcade.release.screens;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.flam.arcade.R;
import com.google.ar.sceneform.ux.ArFragment;

public class LandingScreen {

    public LandingScreen(Context context, ArFragment arFragment) {

        WebView logoView = ((Activity) context).findViewById(R.id.logo);
        FrameLayout landingScreen = ((Activity) context).findViewById(R.id.LandingScreen);
        FrameLayout instructionsView = ((Activity) context).findViewById(R.id.instructionsView);

        // Configure WebView for transparent GIF display
        logoView.getSettings().setLoadWithOverviewMode(true);
        logoView.getSettings().setUseWideViewPort(true);
        logoView.getSettings().setJavaScriptEnabled(true);
        logoView.setBackgroundColor(0x00000000); // Transparent background

        String gifPath = "file:///android_res/raw/logo.gif"; // if stored in res/raw
        String htmlData = "<html><body style='margin:0;padding:0;'><img style='width:100%;height:auto;' src=\"" + gifPath + "\"></body></html>";
        logoView.loadDataWithBaseURL("file:///android_res/raw/", htmlData, "text/html", "UTF-8", null);

        // Delay and then remove the entire landing screen and show instructions
        new Handler().postDelayed(() -> {
            // Clean up WebView
            logoView.loadUrl("about:blank");
            logoView.clearHistory();
            logoView.removeAllViews();
            logoView.destroy();

            // Remove LandingScreen container from root layout
            if (landingScreen != null) {
                ((FrameLayout) landingScreen.getParent()).removeView(landingScreen);
            }

            // Show instructions
            if (instructionsView != null) {
                instructionsView.setVisibility(View.VISIBLE);

                new Instructions(context, arFragment);
            }

        }, 5000); // Delay in ms
    }
}
