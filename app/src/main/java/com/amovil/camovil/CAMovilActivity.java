package com.amovil.camovil;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.amovil.camovil.databinding.ActivityCamovilBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.Executors;

public class CAMovilActivity extends AppCompatActivity {

    ActivityCamovilBinding binding;
    private RelativeLayout photoPreviewLayout;
    private ImageView photoPreview;
    private Button confirmButton, cancelButton;
    private String imagePath = "";
    private ImageButton captureButton, flashButton, flipCameraButton;
    private PreviewView previewView;
    private boolean isBackCamera = true;
    private int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), o -> {
        if (o) {
            startCamera(cameraFacing);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCamovilBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        this.setup();
        if (ContextCompat.checkSelfPermission(CAMovilActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startCamera(cameraFacing);
        }

        flipCameraButton.setOnClickListener(v -> flipCamera());

    }

    private void setup() {
        this.matchJavaWithXml();
        this.setListeners();
    }

    private void matchJavaWithXml() {
        this.previewView = (binding.previewView);
        this.captureButton = (binding.captureButton);
        this.flashButton = (binding.flashButton);
        this.flipCameraButton = (binding.flipCameraButton);
        this.confirmButton = (binding.confirmButton);
        this.cancelButton = (binding.cancelButton);
        this.photoPreviewLayout = (binding.photoPreviewLayout);
        this.photoPreview = (binding.photoPreview);
    }

    private void setListeners() {
        this.confirmButton.setOnClickListener(v -> {
            this.saveAndConfirmImage();
        });

        this.cancelButton.setOnClickListener(v -> {
            this.cancelImage();
        });
    }

    private void saveAndConfirmImage() {
        Intent i = getIntent();
        i.putExtra("imagePath", this.imagePath);
        setResult(RESULT_OK, i);
        Log.i("TAG", "saveAndConfirmImage: " + this.imagePath);
        finish();
    }

    private void cancelImage() {
        if (!this.imagePath.isEmpty()) {
            File file = new File(this.imagePath);
            if (file.exists()) {
                boolean isDeleted = file.delete();
                Log.i("TAG", "cancelImage: " + isDeleted);
            }
        }

        this.photoPreviewLayout.setVisibility(RelativeLayout.GONE);
        this.previewView.setVisibility(PreviewView.VISIBLE);
        this.flashButton.setVisibility(ImageButton.VISIBLE);
        this.flipCameraButton.setVisibility(ImageButton.VISIBLE);
        this.captureButton.setVisibility(Button.VISIBLE);
        this.startCamera(cameraFacing);
    }

    private void flipCamera() {
        if (isBackCamera) {
            cameraFacing = CameraSelector.LENS_FACING_FRONT;
            isBackCamera = false;
        } else {
            cameraFacing = CameraSelector.LENS_FACING_BACK;
            isBackCamera = true;
        }
        startCamera(cameraFacing);
    }

    private void startCamera(int cameraFacing) {
        int aspectRatio = aspectRatio(previewView.getWidth(), previewView.getHeight());

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().setTargetAspectRatio(aspectRatio).build();
                ImageCapture image = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();
                cameraProvider.unbindAll();

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, image);

                captureButton.setOnClickListener(v -> {

                    takePicture(image);
                });

                flashButton.setOnClickListener(v -> {
                    setFlashIcon(camera);
                });

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

            } catch (java.util.concurrent.ExecutionException |
                     java.lang.InterruptedException ignored) {
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private void setFlashIcon(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            if (camera.getCameraInfo().getTorchState().getValue() == 0) {
                camera.getCameraControl().enableTorch(true);
                flashButton.setImageResource(R.drawable.baseline_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                flashButton.setImageResource(R.drawable.baseline_flash_on_24);
            }
        } else {
            runOnUiThread(() -> Toast.makeText(CAMovilActivity.this, "Flash is not available currently", Toast.LENGTH_SHORT).show());
        }
    }

    private void takePicture(ImageCapture imageCapture) {
        final File folders = new File(getCacheDir(), "CustomImages");
        boolean folderCreated = folders.mkdirs();

        if (!folderCreated && !folders.exists()) {
            runOnUiThread(() -> Toast.makeText(CAMovilActivity.this, "Failed to create directory", Toast.LENGTH_SHORT).show());
            return;
        }

        File file = new File(folders, System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(() -> {
                    photoPreview.setImageURI(Uri.fromFile(file));

                    photoPreview.setAdjustViewBounds(true);
                    photoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    imagePath = file.getPath();
                    Log.i("TAG", "onImageSaved: " + imagePath);
                    previewView.setVisibility(PreviewView.GONE);
                    photoPreviewLayout.setVisibility(RelativeLayout.VISIBLE);
                    flashButton.setVisibility(ImageButton.GONE);
                    flipCameraButton.setVisibility(ImageButton.GONE);
                    captureButton.setVisibility(Button.GONE);
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(CAMovilActivity.this, "Failed to save: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
                startCamera(cameraFacing);
            }
        });
    }

    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

}