package com.flam.arcade.release.screens;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;

import com.flam.arcade.MainActivity;
import com.flam.arcade.R;
import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class GameScreen {

    private final Context context;
    private final ArFragment arFragment;
    private final List<Node> sceneNodes = new ArrayList<>();
    private final List<AnchorNode> anchorNodes = new ArrayList<>(); // Track anchor nodes
    private final List<Renderable> loadedModels = new ArrayList<>();
    private final Map<Renderable, String> modelTypeMap = new HashMap<>();
    private final Map<Node, String> nodeModelTypeMap = new HashMap<>();
    private final Set<Node> tappedNodes = new HashSet<>();
    private final Handler respawnHandler = new Handler();
    private boolean isGameActive = false;

    private final Map<Integer, String> imageToModelMap = new HashMap<>();
    private final Handler imageChangeHandler = new Handler();

    // Reference to UI elements
    private MediaPlayer tapSound, countdownSound, changeSound;
    private int currentScore = 0;
    private CountDownTimer gameTimer;
    private int remainingGameTime = 0;

    private final TextView timerText;
    private final TextView scoreText;

    private int currentTargetImageId = -1; // holds the currently displayed image drawable id

    // Respawn delay in milliseconds
    private static final int RESPAWN_DELAY = 1500; // 1.5 seconds

    // Model URIs
    private static final String BURGER_MODEL = "models/Burger.glb";
    private static final String COKE = "models/Coke.glb";
    private static final String ICE_CREAM = "models/IceCream.glb";
    private static final String MCPUFF = "models/mcPuff.glb";
    private static final String FRIES = "models/Fries.glb";
    private static final String MCWRAP = "models/mcWrap.glb";
    private static final String NUGGET = "models/Nuggets.glb";

    private boolean hasPlacedModels = false;

    ImageView restartButton;

    public GameScreen(Context context, ArFragment arFragment) {
        this.context = context;
        this.arFragment = arFragment;

        // Set up UI references
        timerText = ((Activity) context).findViewById(R.id.timerText);
        scoreText = ((Activity) context).findViewById(R.id.scoreText);
        restartButton = ((Activity) context).findViewById(R.id.restartButton);

        ((MainActivity) context).bgmPlayer.setVolume(0.1f,0.1f);


        // Initialize the image to model map
        imageToModelMap.put(R.drawable.homebutton, BURGER_MODEL);
        imageToModelMap.put(R.drawable.coke, COKE);
        imageToModelMap.put(R.drawable.ice_cream, ICE_CREAM);
        imageToModelMap.put(R.drawable.mc_wrap, MCWRAP);
        imageToModelMap.put(R.drawable.fries, FRIES);
        imageToModelMap.put(R.drawable.nuggets, NUGGET);
//        imageToModelMap.put(R.drawable.mc_puff,MCPUFF);


        restartButton.setOnClickListener(v -> {

                restart();
                startGameTimer(45);
                arFragment.getArSceneView().getScene().addOnUpdateListener(this::onSceneUpdate);

        });

        // Set up the scene frame listener to place models once AR session is ready
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onSceneUpdate);
    }

    public void loadModels() {
        String[] modelUris = {
                BURGER_MODEL,
                COKE,
                ICE_CREAM,
                NUGGET,
                FRIES,
        };

        WeakReference<GameScreen> weakReference = new WeakReference<>(this);

        for (String uri : modelUris) {
            ModelRenderable.builder()
                    .setSource(this.context, Uri.parse(uri))
                    .setIsFilamentGltf(true)
                    .setAsyncLoadEnabled(false)
                    .build()
                    .thenAccept(renderable -> {
                        GameScreen gameScreen = weakReference.get();
                        if (gameScreen != null) {
                            loadedModels.add(renderable);

                            // Map model to its type
                            gameScreen.modelTypeMap.put(renderable, uri);
                        }
                    })
                    .exceptionally(throwable -> {
                        Toast.makeText(this.context, "Failed to load " + uri, Toast.LENGTH_LONG).show();
                        return null;
                    });
        }
    }

    private void onSceneUpdate(FrameTime frameTime) {
        // Only place models once and when at least the first 3 models are loaded
        if (hasPlacedModels || loadedModels.size() < 3) {
            return;
        }

        // Place models once the AR session is ready and camera is tracking
        if (arFragment.getArSceneView().getArFrame() != null &&
                arFragment.getArSceneView().getArFrame().getCamera().getTrackingState() ==
                        com.google.ar.core.TrackingState.TRACKING) {

            createAnchorAndPlaceModels();
            hasPlacedModels = true;
            isGameActive = true;

            // Remove the update listener once models are placed
            arFragment.getArSceneView().getScene().removeOnUpdateListener(this::onSceneUpdate);
        }
    }

    private void createAnchorAndPlaceModels() {
        // Get camera position and forward vector
        Camera camera = arFragment.getArSceneView().getScene().getCamera();
        Vector3 cameraPosition = camera.getWorldPosition();
        Vector3 forward = camera.getForward();
        Vector3 anchorPosition = Vector3.add(cameraPosition, forward.scaled(0));

        // Create the anchor
        Pose pose = Pose.makeTranslation(anchorPosition.x, anchorPosition.y, anchorPosition.z);
        Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(pose);

        // Create an anchor node
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        anchorNodes.add(anchorNode);

        // Place models around the anchor
        placeModelsAroundAnchor(anchorNode);
    }

    private void placeModelsAroundAnchor(AnchorNode anchorNode) {
        List<Vector3> spherePoints = generateFrontArcPoints(20, 1.5f, 5f, 150, 120);

        if (loadedModels.isEmpty()) {
            return;
        }

        for (Vector3 pos : spherePoints) {
            int randomModelIndex = (int) (Math.random() * loadedModels.size());
            Renderable model = loadedModels.get(randomModelIndex);

            placeModelAtPosition(model, pos, anchorNode);
        }

        // Start target image rotation
        randomizeTargetImage();
        scheduleTargetImageRotation();
    }

    private void placeModelAtPosition(Renderable model, Vector3 position, AnchorNode anchorNode) {
        Node node = new Node();
        node.setRenderable(model);

        String modelType = modelTypeMap.get(model);
        nodeModelTypeMap.put(node, modelType);

        sceneNodes.add(node);

        node.setParent(anchorNode);
        node.setLocalPosition(position);
        node.setLocalScale(new Vector3(2f, 2f, 2f)); // Make models a bit bigger

        node.setOnTapListener((hitTestResult, motionEvent) -> {
            animateNodeScaleAndRemove(node);
        });
    }

    private void randomizeTargetImage() {
        List<Integer> drawableIds = new ArrayList<>(imageToModelMap.keySet());
        int randomIndex = (int) (Math.random() * drawableIds.size());
        currentTargetImageId = drawableIds.get(randomIndex);

        ImageView homeIcon = ((Activity) context).findViewById(R.id.home);
        homeIcon.setImageResource(currentTargetImageId);

        // Create and apply the scale animation
        animateHomeIcon(homeIcon);
    }

    private void animateHomeIcon(ImageView homeIcon) {
        // Create scale up animation
        ScaleAnimation scaleUp = new ScaleAnimation(
                1.0f, 1.2f, // Start and end X scale
                1.0f, 1.2f, // Start and end Y scale
                Animation.RELATIVE_TO_SELF, 0.0f, // Pivot X position (center)
                Animation.RELATIVE_TO_SELF, 0.0f); // Pivot Y position (center)
        scaleUp.setDuration(300); // Duration in milliseconds

        // Create scale down animation
        ScaleAnimation scaleDown = new ScaleAnimation(
                1.2f, 1.0f, // Start and end X scale
                1.2f, 1.0f, // Start and end Y scale
                Animation.RELATIVE_TO_SELF, 0.0f, // Pivot X position (center)
                Animation.RELATIVE_TO_SELF, 0.0f); // Pivot Y position (center)
        scaleDown.setDuration(300); // Duration in milliseconds

        // Create animation set
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(scaleUp);

        // Start scale down after scale up completes
        scaleUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                homeIcon.startAnimation(scaleDown);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // Start the animation
        homeIcon.startAnimation(scaleUp);
    }

    private void scheduleTargetImageRotation() {
        imageChangeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isGameActive) return;

                changeSound = MediaPlayer.create(context, R.raw.change);
                changeSound.setLooping(false);
                changeSound.setVolume(0.75f, 0.75f);
                changeSound.start();

                randomizeTargetImage(); // change the image

                // Schedule next run randomly between 7–8 seconds (7000–8000 ms)
                int delay = 7000 + (int)(Math.random() * 1000);
                imageChangeHandler.postDelayed(this, delay);
            }
        }, 7000); // initial delay before first change
    }


    private void animateNodeScaleAndRemove(Node node) {
        if (tappedNodes.contains(node)) return;
        tappedNodes.add(node);

        // Find the parent anchor node
        Node parent = (Node) node.getParent();
        while (parent != null && !(parent instanceof AnchorNode)) {
            parent = (Node) parent.getParent();
        }
        AnchorNode parentAnchor = (parent instanceof AnchorNode) ? (AnchorNode) parent : null;

        String modelType = nodeModelTypeMap.get(node);
        int points;

        String correctModel = imageToModelMap.get(currentTargetImageId);
        if (correctModel != null && correctModel.equals(modelType)) {
            points = 100;
            tapSound = MediaPlayer.create(this.context, R.raw.correct);
            tapSound.setLooping(false);
            tapSound.setVolume(0.75f, 0.75f);
            tapSound.start();
        } else {
            points = -50;
            tapSound = MediaPlayer.create(this.context, R.raw.error);
            tapSound.setLooping(false);
            tapSound.setVolume(0.75f, 0.75f);
            tapSound.start();
        }

        updateScore(points);

        // 🎯 Show floating score on screen
        Vector3 worldPos = node.getWorldPosition();
        FrameLayout overlay = ((Activity) context).findViewById(R.id.scoreOverlay);
        showFloatingScore(context, overlay, (points > 0 ? "+" : "") + points, worldPos, arFragment);

        // Handle animation
        Vector3 oldPosition = node.getLocalPosition();
        RenderableInstance renderableInstance = node.getRenderableInstance();

        if (renderableInstance != null && renderableInstance.hasAnimations()) {
            List<String> animations = renderableInstance.getAnimationNames();
            if (!animations.isEmpty()) {
                ObjectAnimator animator = ModelAnimator.ofAnimation(renderableInstance, animations.get(0));
                animator.setRepeatCount(0);
                animator.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        removeNodeAndRespawn(node, oldPosition, parentAnchor);
                    }
                });
                animator.start();
                return;
            }
        }

        removeNodeAndRespawn(node, oldPosition, parentAnchor);
    }

    private void showFloatingScore(Context context, FrameLayout overlay, String text, Vector3 worldPos, ArFragment arFragment) {
        // Convert world to screen position
        Vector3 screen = arFragment.getArSceneView().getScene().getCamera().worldToScreenPoint(worldPos);

        // Create ImageView for score indicator
        ImageView scoreImage = new ImageView(context);

        // Choose image based on score
        @DrawableRes int drawableRes = text.startsWith("+") ? R.drawable.pos : R.drawable.neg;
        scoreImage.setImageResource(drawableRes);

        // Optional: set size and scale
        int size = 250; // px, adjust as needed or use TypedValue.applyDimension
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.leftMargin = (int) screen.x - size / 2;
        params.topMargin = (int) screen.y - size / 2;
        overlay.addView(scoreImage, params);

        // Animate: move up + fade out
        scoreImage.setAlpha(1f);
        scoreImage.animate()
                .translationYBy(-100f)
                .alpha(0f)
                .setDuration(1000)
                .withEndAction(() -> overlay.removeView(scoreImage))
                .start();
    }



    private float[] worldToScreen(ArFragment arFragment, Vector3 worldPos) {
        Session session = arFragment.getArSceneView().getSession();
        if (session == null) return null;

        float[] world = new float[]{ worldPos.x, worldPos.y, worldPos.z };
        float[] ndc = new float[4];
        float[] screenPos = new float[2];

        Camera camera = arFragment.getArSceneView().getScene().getCamera();
        Vector3 screen = camera.worldToScreenPoint(worldPos);
        screenPos[0] = screen.x;
        screenPos[1] = screen.y;
        return screenPos;
    }



    private void removeNodeAndRespawn(Node node, Vector3 position, AnchorNode parentAnchor) {
        node.setParent(null);
        sceneNodes.remove(node);
        nodeModelTypeMap.remove(node);

        // Respawn a new model after delay
        respawnHandler.postDelayed(() -> {
            if (!isGameActive || loadedModels.isEmpty() || parentAnchor == null) return;

            int randomModelIndex = (int) (Math.random() * loadedModels.size());
            Renderable model = loadedModels.get(randomModelIndex);
            placeModelAtPosition(model, position, parentAnchor);
        }, RESPAWN_DELAY);
    }

    public void updateScore(int points) {
        int previousScore = currentScore;
        currentScore += points;
        updateScoreText();

        // Check if player crossed the 1000 points milestone
        if (previousScore < 1000 && currentScore >= 1000) {
            addBonusTime(10);
            showBonusTimeToast(10);
        }

        // Check for additional milestones
        if (previousScore < 2000 && currentScore >= 2000) {
            addBonusTime(10);
            showBonusTimeToast(10);
        }

        // Additional milestones
        if (previousScore < 3000 && currentScore >= 3000) {
            addBonusTime(15);
            showBonusTimeToast(15);
        }

//        // Play coin sound
//        if (points > 0) {
//            coinSound = MediaPlayer.create(this.context, R.raw.coins);
//            coinSound.setLooping(false);
//            coinSound.setVolume(0.7f, 0.7f);
//            coinSound.start();
//        }
    }

    private void updateScoreText() {
        if (scoreText != null) {
            scoreText.setText(String.valueOf(currentScore));
        }
    }

    /**
     * Adds bonus time to the current game timer
     * @param secondsToAdd Number of seconds to add to the timer
     */
    public void addBonusTime(int secondsToAdd) {
        // Cancel existing timer
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        // Add time to the remaining time
        remainingGameTime += secondsToAdd;

        // Create and start a new timer with updated time
        startGameTimerWithRemaining(remainingGameTime);
    }

    public void startGameTimer(int seconds) {
        // Store initial game time
        remainingGameTime = seconds;

        // Start timer with initial seconds
        startGameTimerWithRemaining(seconds);
    }

    private void startGameTimerWithRemaining(int seconds) {
        // Cancel any existing timer
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        // Set game as active
        isGameActive = true;

        // Create and start new timer
        gameTimer = new CountDownTimer(seconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingGameTime = (int) (millisUntilFinished / 1000);

                if (timerText != null) {
                    timerText.setText(String.valueOf(remainingGameTime));
                }

                // Play countdown sound when 3 seconds remain
                if (remainingGameTime == 3) {
                    countdownSound = MediaPlayer.create(context, R.raw.countdown);
                    countdownSound.setLooping(false);
                    countdownSound.setVolume(0.75f, 0.75f);
                    countdownSound.start();
                }
            }

            @Override
            public void onFinish() {
                isGameActive = false;
                clearAllNodes();

                ((MainActivity) context).bgmPlayer.pause();

                MediaPlayer endSound;
                endSound = MediaPlayer.create(context, R.raw.win);
                endSound.setLooping(false);
                endSound.setVolume(0.5f, 0.5f);
                endSound.start();

                // Show final score UI
                showFinalScoreUI();
            }
        }.start();
    }

    /**
     * Animates stars based on the player's score
     * - 1 star: score >= 500
     * - 2 stars: score >= 1250
     * - 3 stars: score >= 2000
     */
    private void animateStars() {
        ImageView star1 = ((Activity) context).findViewById(R.id.star1);
        ImageView star2 = ((Activity) context).findViewById(R.id.star2);
        ImageView star3 = ((Activity) context).findViewById(R.id.star3);

        // Get current score
        TextView scoreText = ((Activity) context).findViewById(R.id.scoreText);
        int score = 0;
        try {
            score = Integer.parseInt(scoreText.getText().toString());
        } catch (NumberFormatException e) {
            // Handle potential parsing error
        }

        // Reset stars to invisible initially
        star1.setVisibility(View.INVISIBLE);
        star2.setVisibility(View.INVISIBLE);
        star3.setVisibility(View.INVISIBLE);

        // Set initial scale to 0 for animation
        star1.setScaleX(0f);
        star1.setScaleY(0f);
        star2.setScaleX(0f);
        star2.setScaleY(0f);
        star3.setScaleX(0f);
        star3.setScaleY(0f);

        // Animate stars based on score thresholds
        if (score >= 500) {
            animateStar(star1, 0, 340);

            if (score >= 1250) {
                animateStar(star2, 300, 360);

                if (score >= 2000) {
                    animateStar(star3, 600, 380);
                }
            }
        }
    }

    /**
     * Animates a single star with scaling effect
     *
     * @param star The ImageView of the star to animate
     * @param delay Delay before starting the animation in milliseconds
     */
    private void animateStar(ImageView star, long delay, int rotation) {
        star.setVisibility(View.VISIBLE);

        // Create scaling animation
        star.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(delay)
                .setInterpolator(new OvershootInterpolator(1.2f))  // Add bounce effect
                .start();

        // Optional: Add rotation animation for more flair
        star.animate()
                .rotation(rotation)
                .setDuration(800)
                .setStartDelay(delay)
                .start();
    }

    private void showFinalScoreUI() {
        Activity activity = (Activity) context;

        animateStars();

        // Hide other UI elements
        activity.findViewById(R.id.gameHelper).setVisibility(View.GONE);
        activity.findViewById(R.id.endScreen).setVisibility(View.VISIBLE);
        activity.findViewById(R.id.restartButton).setVisibility(View.VISIBLE);
        activity.findViewById(R.id.confetti).setVisibility(View.VISIBLE);

        GifImageView confettiView = activity.findViewById(R.id.confetti);
        confettiView.setVisibility(View.VISIBLE);

        if (currentScore > 500) {
            try {
                GifDrawable confettiDrawable = new GifDrawable(context.getResources(), R.raw.confetti);
                confettiDrawable.setLoopCount(1); // Play once
                confettiView.setImageDrawable(confettiDrawable);
                confettiView.setVisibility(View.VISIBLE);
                confettiDrawable.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Set image resource directly for PNG
            confettiView.setImageResource(R.drawable.fail);
            confettiView.setVisibility(View.VISIBLE);
        }



        ImageView scorecard = activity.findViewById(R.id.gamescore);
        TextView scoreText = activity.findViewById(R.id.scoreText);

        scorecard.setVisibility(View.VISIBLE);
        scoreText.setVisibility(View.VISIBLE);

        // Get layout params
        RelativeLayout.LayoutParams paramsScorecard = (RelativeLayout.LayoutParams) scorecard.getLayoutParams();
        RelativeLayout.LayoutParams paramsScoreText = (RelativeLayout.LayoutParams) scoreText.getLayoutParams();

        // Convert to bigger size
        paramsScorecard.width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 180, scorecard.getResources().getDisplayMetrics());

        // Begin transition animation
        ViewGroup parent = (ViewGroup) scorecard.getParent();
        TransitionManager.beginDelayedTransition(parent);

        // Update layout rules
        paramsScorecard.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        paramsScorecard.removeRule(RelativeLayout.ALIGN_PARENT_END);
        paramsScorecard.addRule(RelativeLayout.CENTER_HORIZONTAL); // center scorecard

        scorecard.setLayoutParams(paramsScorecard);

        // Update scoreText layout
        paramsScoreText.removeRule(RelativeLayout.ALIGN_BOTTOM);
        paramsScoreText.addRule(RelativeLayout.CENTER_VERTICAL);
        scoreText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        scoreText.setLayoutParams(paramsScoreText);
    }

    /**
     * Shows a special toast for bonus time
     * @param seconds Number of seconds added
     */
    private void showBonusTimeToast(int seconds) {
        Toast toast = Toast.makeText(context, "BONUS: +" + seconds + " seconds!", Toast.LENGTH_LONG);
        View view = toast.getView();

        countdownSound.pause();

        // Customize toast appearance if available on this Android version
        if (view != null) {
            // Set a background color
            view.setBackgroundColor(Color.rgb(0, 200, 0));

            // Find the TextView within the Toast
            TextView text = view.findViewById(android.R.id.message);
            if (text != null) {
                text.setTextColor(Color.RED);
                text.setTextSize(18);
            }
        }

        toast.show();

        // Flash the timer text to indicate bonus
        if (timerText != null) {
            // Flash timer text by changing colors
            ObjectAnimator colorAnim = ObjectAnimator.ofArgb(timerText, "textColor",
                    Color.RED, Color.GREEN, Color.RED);
            colorAnim.setDuration(1000);
            colorAnim.setRepeatCount(1);
            colorAnim.start();
        }
    }

    public void restart() {
        Activity activity = (Activity) context;

        ((MainActivity) context).bgmPlayer.start();
        activity.findViewById(R.id.confetti).setVisibility(View.GONE);


        // Clear the scene
        clearAllNodes();

        // Reset score
        currentScore = 0;
        updateScoreText();

        // Reset tapped nodes tracking
        tappedNodes.clear();

        // Reset remaining time
        remainingGameTime = 0;

        // Set game as active
        isGameActive = true;

        // Reset UI elements
        activity.findViewById(R.id.gameHelper).setVisibility(View.VISIBLE);
        activity.findViewById(R.id.endScreen).setVisibility(View.GONE);
        activity.findViewById(R.id.restartButton).setVisibility(View.GONE);

        ImageView scorecard = activity.findViewById(R.id.gamescore);
        TextView scoreText = activity.findViewById(R.id.scoreText);

        // Get layout params
        RelativeLayout.LayoutParams paramsScorecard = (RelativeLayout.LayoutParams) scorecard.getLayoutParams();
        RelativeLayout.LayoutParams paramsScoreText = (RelativeLayout.LayoutParams) scoreText.getLayoutParams();

        // Convert to original size
        paramsScorecard.width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 160, scorecard.getResources().getDisplayMetrics());

        // Begin transition animation
        ViewGroup parent = (ViewGroup) scorecard.getParent();
        TransitionManager.beginDelayedTransition(parent);

        // Update layout rules
        paramsScorecard.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        paramsScorecard.addRule(RelativeLayout.ALIGN_PARENT_END);
        paramsScorecard.removeRule(RelativeLayout.CENTER_HORIZONTAL);

        scorecard.setLayoutParams(paramsScorecard);

        // Update scoreText layout
        paramsScoreText.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.gamescore);
        paramsScoreText.removeRule(RelativeLayout.CENTER_VERTICAL);
        scoreText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        paramsScoreText.bottomMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 10, scorecard.getResources().getDisplayMetrics());

        scoreText.setLayoutParams(paramsScoreText);

        // Create new models
        hasPlacedModels = false;
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onSceneUpdate);
    }

    public void clearAllNodes() {
        // Clear our tracked nodes
        for (Node node : sceneNodes) {
            if (node != null) {
                node.setParent(null);
                // Also remove from our model type map
                nodeModelTypeMap.remove(node);
            }
        }
        sceneNodes.clear();

        // Clear anchor nodes
        for (AnchorNode anchorNode : anchorNodes) {
            if (anchorNode != null) {
                if (anchorNode.getAnchor() != null) {
                    anchorNode.getAnchor().detach();
                }
                anchorNode.setParent(null);
            }
        }
        anchorNodes.clear();

        // For any other nodes, remove them too (safety)
        if (arFragment != null && arFragment.getArSceneView() != null) {
            List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
            for (Node node : children) {
                // Skip the camera node
                if (!(node.equals(arFragment.getArSceneView().getScene().getCamera()))) {
                    node.setParent(null);
                    // Also remove from our model type map if present
                    nodeModelTypeMap.remove(node);
                }
            }
        }
    }

    /**
     * Generates points in a 3D arc in front of the player
     *
     * @param count Number of points to generate
     * @param minDistance Minimum distance from player (meters)
     * @param maxDistance Maximum distance from player (meters)
     * @param horizontalArcDegrees Horizontal spread angle in degrees (total arc width)
     * @param verticalArcDegrees Vertical spread angle in degrees (total arc height)
     * @return List of Vector3 points in the arc
     */
    private List<Vector3> generateFrontArcPoints(
            int count,
            float minDistance,
            float maxDistance,
            float horizontalArcDegrees,
            float verticalArcDegrees
    ) {
        List<Vector3> points = new ArrayList<>();
        Random rand = new Random();

        float horizontalArcRadians = (float) Math.toRadians(horizontalArcDegrees);
        float verticalArcRadians = (float) Math.toRadians(verticalArcDegrees);

        for (int i = 0; i < count; i++) {
            // Random yaw within horizontal arc
            float yaw = (rand.nextFloat() - 0.5f) * horizontalArcRadians;

            // Random pitch within vertical arc
            float pitch = (rand.nextFloat() - 0.5f) * verticalArcRadians;

            // Random distance
            float step = 0.5f;
            int steps = (int) ((maxDistance - minDistance) / step);
            int randomStep = rand.nextInt(steps + 1); // +1 to include maxDistance
            float distance = minDistance + randomStep * step;

            // Direction (facing -Z)
            float x = (float)(Math.cos(pitch) * Math.sin(yaw));
            float y = (float)(Math.sin(pitch));
            float z = (float)(-Math.cos(pitch) * Math.cos(yaw)); // facing -Z

            Vector3 dir = normalized(new Vector3(x, y, z));
            Vector3 point = scale(dir, distance);
            points.add(point);
        }

        return points;
    }




    private Vector3 normalized(Vector3 v) {
        float magnitude = (float) Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        if (magnitude > 0.00001f) {
            return new Vector3(v.x / magnitude, v.y / magnitude, v.z / magnitude);
        } else {
            return new Vector3(0, 0, 0);
        }
    }

    private Vector3 scale(Vector3 v, float scalar) {
        return new Vector3(v.x * scalar, v.y * scalar, v.z * scalar);
    }

    public void cleanup() {
        isGameActive = false;

        imageChangeHandler.removeCallbacksAndMessages(null);

        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }

        // Remove any pending respawn callbacks
        respawnHandler.removeCallbacksAndMessages(null);

        clearAllNodes();

        tappedNodes.clear();
        nodeModelTypeMap.clear();
    }

    // Add these methods to your GameScreen class

    /**
     * Pauses all active media players in the game.
     * Call this when the app is going to background state.
     */
    public void pauseAllMedia() {
        // Pause background music if it's playing
        if (changeSound != null && changeSound.isPlaying()) {
            changeSound.pause();
        }

        // Pause any active sound effects
        if (tapSound != null && tapSound.isPlaying()) {
            tapSound.pause();
        }

        if (countdownSound != null && countdownSound.isPlaying()) {
            countdownSound.pause();
        }
    }

    /**
     * Resumes background music when returning to the game.
     * Call this when the app comes back to foreground.
     */
//    public void resumeAllMedia() {
//        // Only resume background music if the game is active
//        if (isGameActive && bgmPlayer != null && !bgmPlayer.isPlaying()) {
//            bgmPlayer.start();
//        }
//
//        // Generally don't resume sound effects as they're one-shot sounds
//    }

    /**
     * Releases all media player resources.
     * Call this when the app is being destroyed.
     */
    public void releaseAllMedia() {
        // Release background music
        if (changeSound != null) {
            changeSound.stop();
            changeSound.release();
            changeSound = null;
        }

        // Release sound effects
        if (tapSound != null) {
            tapSound.release();
            tapSound = null;
        }

        if (countdownSound != null) {
            countdownSound.release();
            countdownSound = null;
        }
    }
}