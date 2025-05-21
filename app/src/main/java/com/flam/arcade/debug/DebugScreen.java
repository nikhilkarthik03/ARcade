package com.flam.arcade.debug;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.webkit.WebView;
import android.widget.Toast;

import com.flam.arcade.R;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.rendering.RenderableInstance;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class DebugScreen {

    private final Context context;
    private final ArFragment arFragment;
    private static final String BURGER_MODEL = "models/mcPuff.glb";

    public DebugScreen(Context context, ArFragment arFragment){
        this.context = context;
        this.arFragment = arFragment;
    }

    public void loadModels() {


        WebView confettiView = ((Activity) context).findViewById(R.id.confetti);

        confettiView.getSettings().setLoadWithOverviewMode(true);
        confettiView.getSettings().setUseWideViewPort(true);
        confettiView.getSettings().setJavaScriptEnabled(true);
        confettiView.setBackgroundColor(0x00000000); // Transparent background
        String imagePath = "file:///android_res/raw/confetti.gif";

        String htmlData = "<html>" +
                "<head>" +
                "<style>" +
                "html, body { margin: 0; padding: 0; height: 100%; width: 100%; overflow: hidden; background: transparent; }" +
                "img { height: 100%; width: 100%; object-fit: cover; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<img src=\"" + imagePath + "\" />" +
                "</body>" +
                "</html>";

        confettiView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null);

        String[] modelUris = {
                BURGER_MODEL,
        };

        WeakReference<DebugScreen> weakReference = new WeakReference<>(this);

        for (String uri : modelUris) {
            ModelRenderable.builder()
                    .setSource(this.context, Uri.parse(uri))
                    .setIsFilamentGltf(true)
                    .setAsyncLoadEnabled(false)
                    .build()
                    .thenAccept(renderable -> {
                        DebugScreen debugScreen = weakReference.get();
                        if (debugScreen != null) {
                            Node node = new Node();
                            node.setRenderable(renderable);

                            node.setParent(arFragment.getArSceneView().getScene());
                            node.setWorldPosition(new Vector3(0,0,-0.5f));
                            node.setLocalScale(new Vector3(1f, 1f, 1f));

                        }
                    })
                    .exceptionally(throwable -> {
                        Toast.makeText(this.context, "Failed to load " + uri, Toast.LENGTH_LONG).show();
                        return null;
                    });
        }
    }

    public void clean() {
        List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
        for (Node node : children) {
            if (!(node.equals(arFragment.getArSceneView().getScene().getCamera()))) {
                node.setParent(null);
            }
        }
    }
}
