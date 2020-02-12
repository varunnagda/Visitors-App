package com.varun.testapp.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.varun.testapp.R;
import com.varun.testapp.ui.singletons.Visitors;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class
VisitorProfileActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"};
    TextureView textureView;
    private Uri imgUrl;
    ImageView visitorImageView;
    ImageButton imgDeleteButton,changeCameraButton;
    private CameraX.LensFacing lensFacing = CameraX.LensFacing.FRONT;
    private String mobile,personName,visitorType,uId;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseReference;
    private boolean faceDetected;
    private double  smileProb, rightEyeOpenProb,leftEyeOpenProb;
    private  ProgressDialog  progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_profile);
        textureView = findViewById(R.id.view_finder);
        visitorImageView=findViewById(R.id.profile_image);
        imgDeleteButton=findViewById(R.id.imgDelete);
        changeCameraButton=findViewById(R.id.ChangeCamera);
        mAuth=FirebaseAuth.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        Intent intent = getIntent();
        mobile = intent.getStringExtra("phoneNumber");
        personName= intent.getStringExtra("personName");
        visitorType = intent.getStringExtra("visitorType");
        progressDialog = new ProgressDialog(this);
        uId= intent.getStringExtra("userId");
        if(allPermissionsGranted()){

            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        imgDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             deleteImg();
            }
        });
        changeCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (CameraX.LensFacing.FRONT == lensFacing) {
                    lensFacing = CameraX.LensFacing.BACK;
                    startCamera();
                } else {
                    CameraX.unbindAll();
                    lensFacing = CameraX.LensFacing.FRONT;
                    startCamera();
                }
              //  bindCamera();
            }
        });
    }
    private void startCamera() {

        CameraX.unbindAll();
        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen


        PreviewConfig pConfig = new PreviewConfig.Builder().setLensFacing(lensFacing).setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig);
        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we  have to destroy it first then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output){
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);
                        updateTransform();
                        textureView.setSurfaceTexture(output.getSurfaceTexture());

                    }
                });


        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY).setLensFacing(lensFacing)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        findViewById(R.id.imgCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".png");
                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        imgUrl= Uri.parse(file.getAbsolutePath());
                        Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());


                        ExifInterface ei = null;
                        try {
                            ei = new ExifInterface(String.valueOf(imgUrl));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_UNDEFINED);

                        Bitmap rotatedBitmap = null;
                        switch(orientation) {

                            case ExifInterface.ORIENTATION_ROTATE_90:
                                rotatedBitmap = rotateImage(myBitmap, 90);
                                break;

                            case ExifInterface.ORIENTATION_ROTATE_180:
                                rotatedBitmap = rotateImage(myBitmap, 180);
                                break;

                            case ExifInterface.ORIENTATION_ROTATE_270:
                                rotatedBitmap = rotateImage(myBitmap, 270);
                                break;

                            case ExifInterface.ORIENTATION_NORMAL:
                            default:
                                rotatedBitmap = myBitmap;
                        }
                        visitorImageView.setImageBitmap(rotatedBitmap);

                        if(detectInImage(rotatedBitmap)){
                            snackBar("Face Detected");
                        uploadFile(Uri.fromFile(new File(file.getAbsolutePath())));
                        visitorImageView.setVisibility(View.VISIBLE);
                     }
                        else {
                            deleteImg();
                            visitorImageView.setVisibility(View.GONE);
                            snackBar("Keep smiling face not Detected!");
                    }
                        }


                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        String msg = "Pic capture failed : " + message;
                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                        if(cause != null){
                            cause.printStackTrace();
                        }
                    }
                });
            }
        });

        //bind to lifecycle:
        CameraX.bindToLifecycle((LifecycleOwner)this, preview, imgCap);
    }
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private void updateTransform(){
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)textureView.getRotation();

        switch(rotation){
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float)rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
    private void uploadFile(Uri filePath) {

            if (filePath !=null) {
                try {
                    uId = mAuth.getCurrentUser().getUid();
                }catch (Exception e){
                    uId="";
                }

                progressDialog.setTitle("Uploading");
                progressDialog.show();
                final StorageReference usersProfileRef;
                if(visitorType.equals("suspicious_user")){
                        usersProfileRef = FirebaseStorage.getInstance().getReference().child("visitorImg/suspicious_users").child(mobile).child(filePath.getLastPathSegment());
                }
                else {
                        usersProfileRef = FirebaseStorage.getInstance().getReference().child("visitorImg/authentic_user").child(uId).child(filePath.getLastPathSegment());
                }

                UploadTask uploadTask = usersProfileRef.putFile(filePath);

                uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {

                    @Override

                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                        progressDialog.create();

                        return usersProfileRef.getDownloadUrl();

                    }

                }).addOnCompleteListener(new OnCompleteListener<Uri>() {

                    @Override

                    public void onComplete(@NonNull Task<Uri> task) {
                        String vType="";
                        if(visitorType.equals("authentic_user")){

                            mDatabaseReference=  mDatabaseReference.child("visitors");
                            int count=1;
                            Visitors visitors = new Visitors(personName,visitorType,mobile,uId,count,task.getResult().toString());
                            mDatabaseReference.push().setValue(visitors);
                            progressDialog.dismiss();
                            snackBar("You  registered as visitor");

                            vType=visitorType;
                        }
                        else if(visitorType.equals("suspicious_user")) {
                            mDatabaseReference=  mDatabaseReference.child("suspicious_user");
                            int count=1;
                            Visitors visitors = new Visitors(personName,visitorType,mobile,uId,count,task.getResult().toString());
                            mDatabaseReference.push().setValue(visitors);
                            progressDialog.dismiss();
                            snackBar("You  registered as suspicious visitor");
                            vType=visitorType;
                        }

                        finish();
                        mAuth.getInstance().signOut();
                        Intent intent = new Intent(VisitorProfileActivity.this,MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("vtype",vType);
                        startActivity(intent);

                    }

                }).addOnFailureListener(new OnFailureListener() {

                    @Override

                    public void onFailure(@NonNull Exception e) {

                        progressDialog.dismiss();
                    }

                });

            }
    }

     boolean detectInImage(Bitmap bitmap){
         FirebaseVisionFaceDetectorOptions highAccuracyOpts =
                 new FirebaseVisionFaceDetectorOptions.Builder()

                         .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                         .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                         .setMinFaceSize(0.15f)
                         .build();
         FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

         FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                 .getVisionFaceDetector(highAccuracyOpts);
         final Task<List<FirebaseVisionFace>> result =
                 detector.detectInImage(image)
                         .addOnSuccessListener(
                                 new OnSuccessListener<List<FirebaseVisionFace>>() {
                                     @Override
                                     public void onSuccess(List<FirebaseVisionFace> faces) {
                                         faceDetected=true;
                                         }




                                 })
                         .addOnFailureListener(
                                 new OnFailureListener() {
                                     @Override
                                     public void onFailure(@NonNull Exception e) {
                                         // Task failed with an exception
                                         // ...
                                         faceDetected=false;
                                     }
                                 });
         return faceDetected;
     }
     void deleteImg(){
         if(imgUrl!=null){
             File fdelete = new File(String.valueOf(imgUrl));
             if (fdelete.exists() && imgUrl!=null) {
                 if (fdelete.delete()) {
                   //  System.out.println("file Deleted :" + imgUrl);
                     visitorImageView.setVisibility(View.GONE);
                 } else {
                     System.out.println("file not Deleted :" + imgUrl);
                 }
             }}
     }
    void snackBar(String msg){
        final ConstraintLayout constraintLayout =  findViewById(R.id
                .constraintLayout);
        Snackbar snackbar= Snackbar.make(constraintLayout, msg, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        TextView textView=snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(getResources().getColor(R.color.colorBlack));
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        textView.setTextSize(16);
        snackbar.getView().setBackgroundColor(getResources().getColor(R.color.white));
        snackbar.show();
    }

}
