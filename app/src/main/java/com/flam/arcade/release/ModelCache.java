//package com.flam.arcade.release.screens;
//
//import android.animation.ObjectAnimator;
//import android.app.Activity;
//import android.content.Context;
//import android.graphics.Color;
//import android.media.MediaPlayer;
//import android.net.Uri;
//import android.os.CountDownTimer;
//import android.os.Handler;
//import android.transition.TransitionManager;
//import android.util.TypedValue;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.RelativeLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import com.flam.arcade.R;
//import com.google.ar.core.Anchor;
//import com.google.ar.core.Pose;
//import com.google.ar.sceneform.AnchorNode;
//import com.google.ar.sceneform.Camera;
//import com.google.ar.sceneform.FrameTime;
//import com.google.ar.sceneform.Node;
//import com.google.ar.sceneform.animation.ModelAnimator;
//import com.google.ar.sceneform.math.Quaternion;
//import com.google.ar.sceneform.math.Vector3;
//import com.google.ar.sceneform.rendering.ModelRenderable;
//import com.google.ar.sceneform.rendering.Renderable;
//import com.google.ar.sceneform.rendering.RenderableInstance;
//import com.google.ar.sceneform.ux.ArFragment;
//
//import java.lang.ref.WeakReference;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//public class GameScreen {
//
//    private final Context context;
//    private final ArFragment arFragment;
//    private final List<Node> sceneNodes = new ArrayList<>();
//    private final List<AnchorNode> anchorNodes = new ArrayList<>(); // Track anchor nodes
//    private final List<Renderable> loadedModels = new ArrayList<>();
//    private final Map<Renderable, String> modelTypeMap = new HashMap<>();
//    private final Map<Node, String> nodeModelTypeMap = new HashMap<>();
//    private final Set<Node> tappedNodes = new HashSet<>();
//    private final Handler respawnHandler = new Handler();
//    private boolean isGameActive = false;
//
//    private final Map<Integer, String> imageToModelMap = new HashMap<>();
//    private final Handler imageChangeHandler = new Handler();
//
//    // Reference to UI elements
//    private MediaPlayer tapSound, coinSound, countdownSound;
//    private int currentScore = 0;
//    private CountDownTimer gameTimer;
//    private int remainingGameTime = 0;
//
//    private final TextView timerText;
//    private final TextView scoreText;
//
//    private int currentTargetImageId = -1; // holds the currently displayed image drawable id
//
//    // Respawn delay in milliseconds
//    private static final int RESPAWN_DELAY = 1500; // 1.5 seconds
//
//    // Model URIs
//    private static final String BURGER_MODEL = "models/Burger_Blast.glb";
//    private static final String COKE = "models/prop_coke.glb";
//    private static final String ICE_CREAM = "models/prop_ice_cream.glb";
//    private static final String WINGS = "models/prop_chicken_tub.glb";
//    private static final String MCPUFF = "models/prop_mc_puff.glb";
//    private static final String MCWRAP = "models/prop_mc_wrap.glb";
//    private static final String NUGGET = "models/prop_nuggets.glb";
//
//    private boolean hasPlacedModels = false;
//
//    public GameScreen(Context context, ArFragment arFragment) {
//        this.context = context;
//        this.arFragment = arFragment;
//
//        // Set up UI references
//        timerText = ((Activity) context).findViewById(R.id.timerText);
//        scoreText = ((Activity) context).findViewById(R.id.scoreText);
//
//        // Initialize the image to model map
//        imageToModelMap.put(R.drawable.homebutton, BURGER_MODEL);
//        imageToModelMap.put(R.drawable.coke, COKE);
//        imageToModelMap.put(R.drawable.wings, WINGS);
//
//        // Initialize sound pool
//        initSoundPool();
//        initNodePool();
//
//        // Set up the scene frame listener to place models once AR session is ready
//        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onSceneUpdate);
//    }
//
//    public void loadModels() {
//        String[] modelUris = {
//                BURGER_MODEL,
//                COKE,
//                WINGS,
//        };
//
//        WeakReference<GameScreen> weakReference = new WeakReference<>(this);
//
//        for (String uri : modelUris) {
//            ModelRenderable.builder()
//                    .setSource(this.context, Uri.parse(uri))
//                    .setIsFilamentGltf(true)
//                    .setAsyncLoadEnabled(false)
//                    .build()
//                    .thenAccept(renderable -> {
//                        GameScreen gameScreen = weakReference.get();
//                        if (gameScreen != null) {
//                            loadedModels.add(renderable);
//
//                            // Map model to its type
//                            gameScreen.modelTypeMap.put(renderable, uri);
//                        }
//                    })
//                    .exceptionally(throwable -> {
//                        Toast.makeText(this.context, "Failed to load " + uri, Toast.LENGTH_LONG).show();
//                        return null;
//                    });
//        }
//    }
//
//    private void onSceneUpdate(FrameTime frameTime) {
//        // Only place models once and when at least the first 3 models are loaded
//        if (hasPlacedModels || loadedModels.size() < 3) {
//            return;
//        }
//
//        // Place models once the AR session is ready and camera is tracking
//        if (arFragment.getArSceneView().getArFrame() != null &&
//                arFragment.getArSceneView().getArFrame().getCamera().getTrackingState() ==
//                        com.google.ar.core.TrackingState.TRACKING) {
//
//            createAnchorAndPlaceModels();
//            hasPlacedModels = true;
//            isGameActive = true;
//
//            // Remove the update listener once models are placed
//            arFragment.getArSceneView().getScene().removeOnUpdateListener(this::onSceneUpdate);
//        }
//    }
//
//    private void createAnchorAndPlaceModels() {
//        // Get camera position and forward vector
//        Camera camera = arFragment.getArSceneView().getScene().getCamera();
//        Vector3 cameraPosition = camera.getWorldPosition();
//        Vector3 forward = camera.getForward();
//        Vector3 anchorPosition = Vector3.add(cameraPosition, forward.scaled(0));
//
//        // Create the anchor
//        Pose pose = Pose.makeTranslation(anchorPosition.x, anchorPosition.y, anchorPosition.z);
//        Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(pose);
//
//        // Create an anchor node
//        AnchorNode anchorNode = new AnchorNode(anchor);
//        anchorNode.setParent(arFragment.getArSceneView().getScene());
//        anchorNodes.add(anchorNode);
//
//        // Place models around the anchor
//        placeModelsAroundAnchor(anchorNode);
//    }
//
//    private void placeModelsAroundAnchor(AnchorNode anchorNode) {
//        List<Vector3> spherePoints = generateSpherePoints(10, 1.5f);
//
//        if (loadedModels.isEmpty()) {
//            return;
//        }
//
//        for (Vector3 pos : spherePoints) {
//            int randomModelIndex = (int) (Math.random() * loadedModels.size());
//            Renderable model = loadedModels.get(randomModelIndex);
//
//            placeModelAtPosition(model, pos, anchorNode);
//        }
//
//        // Start target image rotation
//        randomizeTargetImage();
//        scheduleTargetImageRotation();
//    }
//
//    // Add this method to get a node from the pool
//    private Node getNodeFromPool() {
//        // Try to find an inactive node in the pool
//        for (Node node : nodePool) {
//            if (!nodePoolActive.get(node)) {
//                nodePoolActive.put(node, true);
//                return node;
//            }
//        }
//
//        // If no nodes available in pool, create a new one
//        Node newNode = new Node();
//        nodePool.add(newNode);
//        nodePoolActive.put(newNode, true);
//        return newNode;
//    }
//
//    // Replace placeModelAtPosition method with this optimized version
//    private void placeModelAtPosition(Renderable model, Vector3 position, AnchorNode anchorNode) {
//        // Get a node from the pool or create one if needed
//        Node node = getNodeFromPool();
//
//        node.setRenderable(model);
//
//        String modelType = modelTypeMap.get(model);
//        nodeModelTypeMap.put(node, modelType);
//
//        sceneNodes.add(node);
//
//        node.setParent(anchorNode);
//        node.setLocalPosition(position);
//
//        // Reduced scale for better performance
//        node.setLocalScale(new Vector3(1.5f, 1.5f, 1.5f));
//
//        node.setOnTapListener((hitTestResult, motionEvent) -> {
//            animateNodeScaleAndRemove(node);
//        });
//    }
//
//    private void returnNodeToPool(Node node) {
//        if (node == null) return;
//
//        // Clear node state
//        node.setParent(null);
//        node.setRenderable(null);
//
//        // Return to pool if it's one of our pooled nodes
//        if (nodePoolActive.containsKey(node)) {
//            nodePoolActive.put(node, false);
//        }
//
//        // Remove from tracking collections
//        sceneNodes.remove(node);
//        nodeModelTypeMap.remove(node);
//        tappedNodes.remove(node);
//    }
//
//    private void randomizeTargetImage() {
//        List<Integer> drawableIds = new ArrayList<>(imageToModelMap.keySet());
//        int randomIndex = (int) (Math.random() * drawableIds.size());
//        currentTargetImageId = drawableIds.get(randomIndex);
//
//        ImageView homeIcon = ((Activity) context).findViewById(R.id.home);
//        homeIcon.setImageResource(currentTargetImageId);
//    }
//
//    private void scheduleTargetImageRotation() {
//        // Increase rotation interval for better performance
//        imageChangeHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (!isGameActive) return;
//
//                randomizeTargetImage(); // change the image
//
//                // Increase delay to reduce UI updates - 5 seconds instead of 3-4
//                imageChangeHandler.postDelayed(this, 5000);
//            }
//        }, 2000); // increased initial delay
//    }
//
//    private static final int POOL_SIZE = 3;
//    private final MediaPlayer[] soundPool = new MediaPlayer[POOL_SIZE];
//    private int currentSoundIndex = 0;
//
//    // Add this method to initialize sound pool
//    private void initSoundPool() {
//        for (int i = 0; i < POOL_SIZE; i++) {
//            soundPool[i] = new MediaPlayer();
//        }
//    }
//
//    private void playSound(int resourceId) {
//        try {
//            MediaPlayer player = soundPool[currentSoundIndex];
//            if (player.isPlaying()) {
//                player.stop();
//            }
//            player.reset();
//            player.setDataSource(context, android.net.Uri.parse("android.resource://" +
//                    context.getPackageName() + "/" + resourceId));
//            player.prepare();
//            player.setVolume(0.7f, 0.7f);
//            player.setLooping(false);
//            player.start();
//
//            // Move to next player in pool
//            currentSoundIndex = (currentSoundIndex + 1) % POOL_SIZE;
//        } catch (Exception e) {
//            // Log error but don't crash
//        }
//    }
//
//    private void animateNodeScaleAndRemove(Node node) {
//        if (tappedNodes.contains(node)) return;
//        tappedNodes.add(node);
//
//        // Find the parent anchor node
//        Node parent = (Node) node.getParent();
//        while (parent != null && !(parent instanceof AnchorNode)) {
//            parent = (Node) parent.getParent();
//        }
//        AnchorNode parentAnchor = (parent instanceof AnchorNode) ? (AnchorNode) parent : null;
//
//        String modelType = nodeModelTypeMap.get(node);
//        int points;
//
//        String correctModel = imageToModelMap.get(currentTargetImageId);
//        if (correctModel != null && correctModel.equals(modelType)) {
//            points = 100;
//            // Use pooled sound player instead of creating new one
//            playSound(R.raw.pos);
//            Toast.makeText(context, "+100 Points!", Toast.LENGTH_SHORT).show();
//        } else {
//            points = -50;
//            // Use pooled sound player instead of creating new one
//            playSound(R.raw.neg);
//            Toast.makeText(context, "-50 Points!", Toast.LENGTH_SHORT).show();
//        }
//
//        updateScore(points);
//
//        Vector3 oldPosition = node.getLocalPosition();
//
//        // Simplify animation - replace complex animation with simple scale down
//        node.setLocalScale(new Vector3(0.01f, 0.01f, 0.01f));
//
//        // Use a single handler instead of creating new ones
//        removeNodeAndRespawn(node, oldPosition, parentAnchor);
//    }
//    private void removeNodeAndRespawn(Node node, Vector3 position, AnchorNode parentAnchor) {
//        // Return node to pool instead of just removing
//        returnNodeToPool(node);
//
//        // Respawn a new model after delay
//        respawnHandler.postDelayed(() -> {
//            if (!isGameActive || loadedModels.isEmpty() || parentAnchor == null) return;
//
//            int randomModelIndex = (int) (Math.random() * loadedModels.size());
//            Renderable model = loadedModels.get(randomModelIndex);
//            placeModelAtPosition(model, position, parentAnchor);
//        }, RESPAWN_DELAY);
//    }
//
//    public void updateScore(int points) {
//        int previousScore = currentScore;
//        currentScore += points;
//        updateScoreText();
//
//        // Check if player crossed the 1000 points milestone
//        if (previousScore < 1000 && currentScore >= 1000) {
//            addBonusTime(10);
//            showBonusTimeToast(10);
//        }
//
//        // Check for additional milestones
//        if (previousScore < 2000 && currentScore >= 2000) {
//            addBonusTime(10);
//            showBonusTimeToast(10);
//        }
//
//        // Additional milestones
//        if (previousScore < 3000 && currentScore >= 3000) {
//            addBonusTime(15);
//            showBonusTimeToast(15);
//        }
//
////        // Play coin sound
////        if (points > 0) {
////            coinSound = MediaPlayer.create(this.context, R.raw.coins);
////            coinSound.setLooping(false);
////            coinSound.setVolume(0.7f, 0.7f);
////            coinSound.start();
////        }
//    }
//
//    private void updateScoreText() {
//        if (scoreText != null) {
//            scoreText.setText(String.valueOf(currentScore));
//        }
//    }
//
//    /**
//     * Adds bonus time to the current game timer
//     * @param secondsToAdd Number of seconds to add to the timer
//     */
//    public void addBonusTime(int secondsToAdd) {
//        // Cancel existing timer
//        if (gameTimer != null) {
//            gameTimer.cancel();
//        }
//
//        // Add time to the remaining time
//        remainingGameTime += secondsToAdd;
//
//        // Create and start a new timer with updated time
//        startGameTimerWithRemaining(remainingGameTime);
//    }
//
//    public void startGameTimer(int seconds) {
//        // Store initial game time
//        remainingGameTime = seconds;
//
//        // Start timer with initial seconds
//        startGameTimerWithRemaining(seconds);
//    }
//
//    private void startGameTimerWithRemaining(int seconds) {
//        // Cancel any existing timer
//        if (gameTimer != null) {
//            gameTimer.cancel();
//        }
//
//        // Set game as active
//        isGameActive = true;
//
//        // Create and start new timer
//        gameTimer = new CountDownTimer(seconds * 1000L, 1000L) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//                remainingGameTime = (int) (millisUntilFinished / 1000);
//
//                if (timerText != null) {
//                    timerText.setText(String.valueOf(remainingGameTime));
//                }
//
//                // Play countdown sound when 3 seconds remain
//                if (remainingGameTime == 3) {
//                    countdownSound = MediaPlayer.create(context, R.raw.countdown);
//                    countdownSound.setLooping(false);
//                    countdownSound.setVolume(0.75f, 0.75f);
//                    countdownSound.start();
//                }
//            }
//
//            @Override
//            public void onFinish() {
//                isGameActive = false;
//                clearAllNodes();
//
//                MediaPlayer endSound;
//                endSound = MediaPlayer.create(context, R.raw.win);
//                endSound.setLooping(false);
//                endSound.setVolume(0.5f, 0.5f);
//                endSound.start();
//
//                // Show final score UI
//                showFinalScoreUI();
//            }
//        }.start();
//    }
//
//    private static final int NODE_POOL_SIZE = 15;
//    private final List<Node> nodePool = new ArrayList<>();
//    private final Map<Node, Boolean> nodePoolActive = new HashMap<>();
//
//    // Add this initialization method
//    private void initNodePool() {
//        for (int i = 0; i < NODE_POOL_SIZE; i++) {
//            Node node = new Node();
//            nodePool.add(node);
//            nodePoolActive.put(node, false);
//        }
//    }
//
//
//    private void animateStars() {
//        ImageView star1 = ((Activity) context).findViewById(R.id.star1);
//        ImageView star2 = ((Activity) context).findViewById(R.id.star2);
//        ImageView star3 = ((Activity) context).findViewById(R.id.star3);
//
//        // Set initial state
//        star1.setVisibility(View.VISIBLE);
//        star2.setVisibility(View.VISIBLE);
//        star3.setVisibility(View.VISIBLE);
//
//        // Sequential animations instead of parallel
//        new Handler().postDelayed(() -> {
//            star1.setScaleX(1f);
//            star1.setScaleY(1f);
//
//            new Handler().postDelayed(() -> {
//                star2.setScaleX(1f);
//                star2.setScaleY(1f);
//
//                new Handler().postDelayed(() -> {
//                    star3.setScaleX(1f);
//                    star3.setScaleY(1f);
//                }, 300);
//            }, 300);
//        }, 300);
//    }
//
//    private void animateStar(ImageView star, long delay) {
//        star.setVisibility(View.VISIBLE);
//        star.animate()
//                .scaleX(1f)
//                .scaleY(1f)
//                .setDuration(400)
//                .setStartDelay(delay)
//                .start();
//    }
//
//    private void showFinalScoreUI() {
//        Activity activity = (Activity) context;
//
//        // Batch UI changes to reduce redraws
//        activity.runOnUiThread(() -> {
//            // Hide multiple elements at once
//            activity.findViewById(R.id.gameHelper).setVisibility(View.GONE);
//            activity.findViewById(R.id.endScreen).setVisibility(View.VISIBLE);
//            activity.findViewById(R.id.restartButton).setVisibility(View.VISIBLE);
//
//            ImageView scorecard = activity.findViewById(R.id.gamescore);
//            TextView scoreText = activity.findViewById(R.id.scoreText);
//
//            scorecard.setVisibility(View.VISIBLE);
//            scoreText.setVisibility(View.VISIBLE);
//
//            // Get layout params
//            RelativeLayout.LayoutParams paramsScorecard = (RelativeLayout.LayoutParams) scorecard.getLayoutParams();
//            RelativeLayout.LayoutParams paramsScoreText = (RelativeLayout.LayoutParams) scoreText.getLayoutParams();
//
//            // Convert to bigger size
//            paramsScorecard.width = (int) TypedValue.applyDimension(
//                    TypedValue.COMPLEX_UNIT_DIP, 150, scorecard.getResources().getDisplayMetrics());
//
//            // Skip TransitionManager for better performance
//            // TransitionManager.beginDelayedTransition(parent);
//
//            // Update layout rules
//            paramsScorecard.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//            paramsScorecard.removeRule(RelativeLayout.ALIGN_PARENT_END);
//            paramsScorecard.addRule(RelativeLayout.CENTER_HORIZONTAL);
//
//            scorecard.setLayoutParams(paramsScorecard);
//
//            // Update scoreText layout
//            paramsScoreText.removeRule(RelativeLayout.ALIGN_BOTTOM);
//            paramsScoreText.addRule(RelativeLayout.CENTER_VERTICAL);
//            scoreText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
//            scoreText.setLayoutParams(paramsScoreText);
//
//            // Simplified star animation - animate one at a time
//            animateStars();
//        });
//    }
//    /**
//     * Shows a special toast for bonus time
//     * @param seconds Number of seconds added
//     */
//    private void showBonusTimeToast(int seconds) {
//        Toast toast = Toast.makeText(context, "BONUS: +" + seconds + " seconds!", Toast.LENGTH_LONG);
//        View view = toast.getView();
//
//        // Customize toast appearance if available on this Android version
//        if (view != null) {
//            // Set a background color
//            view.setBackgroundColor(Color.rgb(0, 200, 0));
//
//            // Find the TextView within the Toast
//            TextView text = view.findViewById(android.R.id.message);
//            if (text != null) {
//                text.setTextColor(Color.RED);
//                text.setTextSize(18);
//            }
//        }
//
//        toast.show();
//
//        // Flash the timer text to indicate bonus
//        if (timerText != null) {
//            // Flash timer text by changing colors
//            ObjectAnimator colorAnim = ObjectAnimator.ofArgb(timerText, "textColor",
//                    Color.RED, Color.GREEN, Color.RED);
//            colorAnim.setDuration(1000);
//            colorAnim.setRepeatCount(1);
//            colorAnim.start();
//        }
//    }
//
//    public void restart() {
//        Activity activity = (Activity) context;
//
//        // Clear the scene
//        clearAllNodes();
//
//        // Reset score
//        currentScore = 0;
//        updateScoreText();
//
//        // Reset tapped nodes tracking
//        tappedNodes.clear();
//
//        // Reset remaining time
//        remainingGameTime = 0;
//
//        // Set game as active
//        isGameActive = true;
//
//        // Reset UI elements
//        activity.findViewById(R.id.gameHelper).setVisibility(View.VISIBLE);
//        activity.findViewById(R.id.endScreen).setVisibility(View.GONE);
//        activity.findViewById(R.id.restartButton).setVisibility(View.GONE);
//
//        ImageView scorecard = activity.findViewById(R.id.gamescore);
//        TextView scoreText = activity.findViewById(R.id.scoreText);
//
//        // Get layout params
//        RelativeLayout.LayoutParams paramsScorecard = (RelativeLayout.LayoutParams) scorecard.getLayoutParams();
//        RelativeLayout.LayoutParams paramsScoreText = (RelativeLayout.LayoutParams) scoreText.getLayoutParams();
//
//        // Convert to original size
//        paramsScorecard.width = (int) TypedValue.applyDimension(
//                TypedValue.COMPLEX_UNIT_DIP, 100, scorecard.getResources().getDisplayMetrics());
//
//        // Begin transition animation
//        ViewGroup parent = (ViewGroup) scorecard.getParent();
//        TransitionManager.beginDelayedTransition(parent);
//
//        // Update layout rules
//        paramsScorecard.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//        paramsScorecard.addRule(RelativeLayout.ALIGN_PARENT_END);
//        paramsScorecard.removeRule(RelativeLayout.CENTER_HORIZONTAL);
//
//        scorecard.setLayoutParams(paramsScorecard);
//
//        // Update scoreText layout
//        paramsScoreText.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.gamescore);
//        paramsScoreText.removeRule(RelativeLayout.CENTER_VERTICAL);
//        scoreText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
//        paramsScoreText.bottomMargin = (int) TypedValue.applyDimension(
//                TypedValue.COMPLEX_UNIT_SP, 6, scorecard.getResources().getDisplayMetrics());
//
//        scoreText.setLayoutParams(paramsScoreText);
//
//        // Create new models
//        hasPlacedModels = false;
//        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onSceneUpdate);
//    }
//
//    public void clearAllNodes() {
//        // Return all nodes to pool
//        for (Node node : new ArrayList<>(sceneNodes)) {
//            returnNodeToPool(node);
//        }
//
//        // Clear anchor nodes
//        for (AnchorNode anchorNode : anchorNodes) {
//            if (anchorNode != null) {
//                if (anchorNode.getAnchor() != null) {
//                    anchorNode.getAnchor().detach();
//                }
//                anchorNode.setParent(null);
//            }
//        }
//        anchorNodes.clear();
//
//        // For any other nodes, remove them too (safety)
//        if (arFragment != null && arFragment.getArSceneView() != null) {
//            List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
//            for (Node node : children) {
//                // Skip the camera node
//                if (!(node.equals(arFragment.getArSceneView().getScene().getCamera()))) {
//                    if (nodePoolActive.containsKey(node)) {
//                        returnNodeToPool(node);
//                    } else {
//                        node.setParent(null);
//                        nodeModelTypeMap.remove(node);
//                    }
//                }
//            }
//        }
//    }
//
//    private List<Vector3> generateSpherePoints(int count, float radius) {
//        List<Vector3> points = new ArrayList<>();
//        double offset = 2.0 / count;
//        double increment = Math.PI * (3.0 - Math.sqrt(5.0));
//
//        for (int i = 0; i < count; i++) {
//            double y = ((i * offset) - 1) + (offset / 2);
//            double r = Math.sqrt(1 - y * y);
//            double phi = i * increment;
//
//            double x = Math.cos(phi) * r;
//            double z = Math.sin(phi) * r;
//
//            points.add(new Vector3((float) (x * radius), (float) (y * radius), (float) (z * radius)));
//        }
//
//        return points;
//    }
//
//    public void cleanup() {
//        isGameActive = false;
//
//        imageChangeHandler.removeCallbacksAndMessages(null);
//
//        if (gameTimer != null) {
//            gameTimer.cancel();
//            gameTimer = null;
//        }
//
//        // Remove any pending respawn callbacks
//        respawnHandler.removeCallbacksAndMessages(null);
//
//        // Clean up sound pool
//        for (MediaPlayer player : soundPool) {
//            try {
//                if (player != null) {
//                    if (player.isPlaying()) {
//                        player.stop();
//                    }
//                    player.release();
//                }
//            } catch (Exception e) {
//                // Ignore errors during cleanup
//            }
//        }
//
//        clearAllNodes();
//
//        tappedNodes.clear();
//        nodeModelTypeMap.clear();
//    }
//}