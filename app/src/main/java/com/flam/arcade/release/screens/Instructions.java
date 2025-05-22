package com.flam.arcade.release.screens;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.flam.arcade.MainActivity;
import com.flam.arcade.R;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class Instructions {

    private int step = 0;
    private Context context;
    private ArFragment arFragment;

    private FrameLayout container;
    private FrameLayout gameView;
    private GifImageView gifImageView;
    private ImageView nextButton;

    public Instructions(Context context, ArFragment arFragment) {
        this.context = context;
        this.arFragment = arFragment;
        Activity activity = (Activity) context;

        container = activity.findViewById(R.id.instructionsView);
        gameView = activity.findViewById(R.id.gameScreen);
        gifImageView = activity.findViewById(R.id.gifImageView);
        nextButton = activity.findViewById(R.id.nextButton);

        nextButton.setVisibility(View.GONE);
        container.setVisibility(View.VISIBLE);

        // Set listener here AFTER nextButton is initialized
        nextButton.setOnClickListener(v -> {
            step++;
            switch (step) {
                case 2:
                    playTrayGif();
                    nextButton.setImageResource(R.drawable.play_button);
                    break;

                case 3:
                    container.setVisibility(View.GONE);
                    gameView.setVisibility(View.VISIBLE);

                    GameScreen gameScreen = new GameScreen(context, arFragment);

                    if (context instanceof MainActivity) {
                        ((MainActivity) context).setGameScreen(gameScreen);
                    }

                    gameScreen.loadModels();
                    gameScreen.startGameTimer(45);
                    break;
            }
        });

        playFirstGifOnce();
    }


    private void playFirstGifOnce() {
        try {
            GifDrawable gifDrawable = new GifDrawable(context.getResources(), R.raw.fries);
            gifDrawable.setLoopCount(1);
            gifImageView.setImageDrawable(gifDrawable);
            gifImageView.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.GONE);

            // Set width to match_parent initially
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) gifImageView.getLayoutParams();
            params.width = FrameLayout.LayoutParams.MATCH_PARENT;
            gifImageView.setLayoutParams(params);

            gifDrawable.start();

            new Handler().postDelayed(() -> {
                gifImageView.setVisibility(View.GONE);
                showStaticInstructions();
                nextButton.setVisibility(View.VISIBLE);
                step = 1;
            },3000);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showStaticInstructions() {
        // Resize gifImageView to 300sp width
        int widthInPx = (int) (300 * context.getResources().getDisplayMetrics().scaledDensity); // 300sp to px

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) gifImageView.getLayoutParams();
        params.width = widthInPx;
        gifImageView.setLayoutParams(params);

        gifImageView.setImageResource(R.drawable.instructions);
        gifImageView.setVisibility(View.VISIBLE);
    }


    private void playTrayGif() {
        try {
            GifDrawable gifDrawable = new GifDrawable(context.getResources(), R.raw.tray);
            gifDrawable.setLoopCount(0); // infinite or set to 1 for once
            gifImageView.setImageDrawable(gifDrawable);
            gifImageView.setVisibility(View.VISIBLE);
            gifDrawable.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
