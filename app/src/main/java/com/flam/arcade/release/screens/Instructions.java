package com.flam.arcade.release.screens;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.flam.arcade.MainActivity;
import com.flam.arcade.R;
import com.google.ar.sceneform.ux.ArFragment;

public class Instructions {

    private int step = 0;

    public Instructions(Context context, ArFragment arFragment) {
        Activity activity = (Activity) context;

        WebView instructionsView = activity.findViewById(R.id.instructions);
        ImageView nextButton = activity.findViewById(R.id.nextButton);
        FrameLayout container = activity.findViewById(R.id.instructionsView);
        FrameLayout gameView = activity.findViewById(R.id.gameScreen);

        // Show the container
        container.setVisibility(View.VISIBLE);

        // Configure WebView
        instructionsView.getSettings().setLoadWithOverviewMode(true);
        instructionsView.getSettings().setUseWideViewPort(true);
        instructionsView.getSettings().setJavaScriptEnabled(true);
        instructionsView.setBackgroundColor(0x00000000); // Transparent background

        // STEP 0: Start with fries.gif
        loadWebViewContent(instructionsView, "file:///android_res/raw/fries.gif");

        // Handle button click logic
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                step++;

                switch (step) {
                    case 1:
                        // STEP 1: Show instructions.png
                        loadWebViewContent(instructionsView, "file:///android_res/drawable/instructions.png");
                        break;

                    case 2:
                        // STEP 2: Show tray.gif and update button to "Play"
                        loadWebViewContent(instructionsView, "file:///android_res/raw/tray.gif");
                        nextButton.setImageResource(R.drawable.play_button);
                        break;

                    case 3:
                        // STEP 3: Hide everything and start GameScreen
                        container.removeAllViews();
                        container.setVisibility(View.GONE);
                        gameView.setVisibility(View.VISIBLE);

                        GameScreen gameScreen = new GameScreen(context, arFragment);

                        // Store reference to the game screen in MainActivity for lifecycle management
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).setGameScreen(gameScreen);
                        }

                        gameScreen.loadModels();
                        gameScreen.startGameTimer(45);
                        break;
                }
            }
        });
    }

    private void loadWebViewContent(WebView webView, String imagePath) {
        String htmlData = "<html><body style='margin:0;padding:0;'><img style='width:100%;height:auto;' src=\"" + imagePath + "\"></body></html>";

        webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null);
    }
}