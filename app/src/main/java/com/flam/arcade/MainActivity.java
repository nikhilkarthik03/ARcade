package com.flam.arcade;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.flam.arcade.debug.DebugScreen;
import com.flam.arcade.release.screens.GameScreen;
import com.flam.arcade.release.screens.LandingScreen;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.InstructionsController;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener {

    private ArFragment arFragment;
    private GameScreen gameScreen;

    public MediaPlayer bgmPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bgmPlayer = MediaPlayer.create(this, R.raw.bgm); // Use your filename
        bgmPlayer.setLooping(true); // Optional: Loop BGM
        bgmPlayer.setVolume(0.3f, 0.3f); // Optional: Set volume (left, right)

        bgmPlayer.start();
        // Load models depending on build type
        if (BuildConfig.IS_DEBUG) {
            setContentView(R.layout.activity_test);
        } else {
            setContentView(R.layout.activity_main);
        }

        getSupportFragmentManager().addFragmentOnAttachListener(this);
        launchArFragment();
    }

    private void launchArFragment() {
        arFragment = new ArFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.arFragment, arFragment)
                .commit();
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment instanceof ArFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
            arFragment.setOnViewCreatedListener(this);
        }
    }

    @Override
    public void onViewCreated(ArSceneView arSceneView) {
        arFragment.setOnViewCreatedListener(null);

        // Disable plane rendering & tap to place
        arSceneView.getPlaneRenderer().setEnabled(false);
        arFragment.setOnTapArPlaneListener(null);
        arFragment.getInstructionsController().setEnabled(InstructionsController.TYPE_PLANE_DISCOVERY, false);

        // Make planes invisible (if ever drawn)
        arSceneView.getPlaneRenderer().getMaterial().thenAccept(material -> {
            material.setFloat3("color", 0.0f, 0.0f, 0.0f);
            arSceneView.getPlaneRenderer().setVisible(false);
        });

        arSceneView.getRenderer().setDesiredSize(1280, 720); // Reduce render resolution
        arSceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
        arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL); // No frame skipping

        // Configure touch behavior for models
        // This is critical: we allow scene touches but don't handle them at scene level
        // so model taps can work
        arSceneView.getScene().setOnTouchListener((hitTestResult, motionEvent) -> {
            // Return false to not consume the event and allow node tap handlers to work
            return false;
        });

        // Load models depending on build type
        if (BuildConfig.IS_DEBUG) {
            new DebugScreen(this, arFragment).loadModels();
        } else {
            new LandingScreen(this, arFragment);
        }
    }

    // Method to set the current game screen instance
    public void setGameScreen(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        // Optimize AR configuration for performance

        // Disable all features not needed for this game
        config.setDepthMode(Config.DepthMode.DISABLED);
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
        config.setInstantPlacementMode(Config.InstantPlacementMode.DISABLED);
        config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);

        // Add these performance optimizations
        config.setFocusMode(Config.FocusMode.AUTO);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause all media when app is paused
        if (gameScreen != null) {
            gameScreen.pauseAllMedia();
            bgmPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume media when app comes back
        if (gameScreen != null) {
//            gameScreen.resumeAllMedia();
            bgmPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release all media resources
        if (gameScreen != null) {
            gameScreen.releaseAllMedia();

            if (bgmPlayer != null) {
                bgmPlayer.stop();
                bgmPlayer.release();
                bgmPlayer = null;
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void shakeItBaby() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) this.getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) this.getSystemService(VIBRATOR_SERVICE)).vibrate(150);
        }
    }
}