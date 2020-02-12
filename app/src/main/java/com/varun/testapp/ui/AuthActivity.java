package com.varun.testapp.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.varun.testapp.R;
import com.varun.testapp.ui.singletons.Visitors;

import java.util.concurrent.TimeUnit;

import static android.widget.Toast.LENGTH_LONG;
import static com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE;
import static com.google.firebase.auth.PhoneAuthProvider.getCredential;

public class AuthActivity extends AppCompatActivity {
    private String mVerificationId;
    private FirebaseAuth mAuth;
    private EditText editTextCode;
    private   FirebaseUser user;
    private TextView textView;
    private String visitorType,mobile,personName;
    DatabaseReference visitorReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mAuth = FirebaseAuth.getInstance();
        editTextCode = findViewById(R.id.otp);
        textView = findViewById(R.id.numberotpsent);
        visitorReference = FirebaseDatabase.getInstance().getReference("/visitors");
        Intent intent = getIntent();
         mobile = intent.getStringExtra("phoneNumber");
         personName= intent.getStringExtra("personName");
        textView.setText("Enter the code we sent to " + "+" + mobile);
        sendVerificationCode(mobile);
        findViewById(R.id.verify).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = editTextCode.getText().toString().trim();
                if ( code.length() != 6) {
                    editTextCode.setError("Enter valid code");
                    editTextCode.requestFocus();
                    return;
                }

                //verifying the code entered manually
                verifyVerificationCode(code);
            }
        });

    }

    //the method is sending verification code
    private void sendVerificationCode(String mobile) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+" + mobile,
                60,
                TimeUnit.SECONDS,
                TaskExecutors.MAIN_THREAD,
                mCallbacks);
    }


    //the callback to detect the verification status
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {

            //Getting the code sent by SMS
            String code = phoneAuthCredential.getSmsCode();

            if (code != null) {
                editTextCode.setText(code);
                //verifying the code
                verifyVerificationCode(code);
            }
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(AuthActivity.this, e.getMessage(), LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);

            //storing the verification id that is sent to the user
            mVerificationId = s;
        }
    };


    private void verifyVerificationCode(String code) {
        //creating the credential
        try {
            PhoneAuthCredential credential = getCredential(mVerificationId, code);
            //signing the user
            signInWithPhoneAuthCredential(credential);
            visitorType="authentic_user";
        } catch (Exception e) {
            Toast.makeText(this, "incorrect otp", LENGTH_LONG).show();

        }

    }

    private void signInWithPhoneAuthCredential(final PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(AuthActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        // Sign in success, update UI with the signed-in user's information

                        try {
                          user = task.getResult().getUser();
                            visitorReference.orderByChild("uid").equalTo(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    int visitorCount=0;

                                    if(dataSnapshot.exists()){
                                        String key=null;
                                        DatabaseReference databaseReference;
                                        databaseReference= FirebaseDatabase.getInstance().getReference("/visitors");
                                        for(DataSnapshot ds : dataSnapshot.getChildren()) {
                                        Visitors visitors =ds.getValue(Visitors.class);
                                        try{
                                            visitorCount = visitors.getVisitor_count();}
                                        catch (Exception e) {
                                            Toast.makeText(getApplicationContext(),"something went wrong.", LENGTH_LONG).show();
                                        }
                                            key = ds.getKey();
                                       }
                                        if (key!=null && visitorCount!=0){
                                            visitorCount=visitorCount+1;
                                            databaseReference.child(key).child("visitor_count").setValue(visitorCount);
                                            snackBar(visitorCount);
                                            mAuth.signOut();
                                            Intent intent = new Intent(AuthActivity.this,MainActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            intent.putExtra("response",visitorCount);
                                            startActivity(intent);
                                        }

                                    }

                                    else {
                                        Intent intent = new Intent(AuthActivity.this, VisitorProfileActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        intent.putExtra("phoneNumber","+"+mobile);
                                        intent.putExtra("personName",personName);
                                        intent.putExtra("visitorType",visitorType);
                                        intent.putExtra("userId",user);
                                        startActivity(intent);
                                    }

                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        } catch (Exception e) {
                            visitorType="suspicious_user";
                            final LinearLayout linearLayout =  findViewById(R.id
                                    .linearLayout);
                            Snackbar snackbar= Snackbar.make(linearLayout, "incorrect otp", Snackbar.LENGTH_LONG);
                            View snackbarView = snackbar.getView();
                            TextView textView=snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                            textView.setTextColor(getResources().getColor(R.color.red));
                            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            snackbar.getView().setBackgroundColor(getResources().getColor(R.color.white));
                            textView.setTextSize(16);
                            snackbar.setDuration(BaseTransientBottomBar.LENGTH_INDEFINITE);
                            snackbar.setAction("Face Verify ", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(AuthActivity.this, VisitorProfileActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    intent.putExtra("phoneNumber","+"+mobile);
                                    intent.putExtra("personName",personName);
                                    intent.putExtra("visitorType",visitorType);
                                    intent.putExtra("userId",user);
                                    startActivity(intent);
                                }
                            });
                            snackbar.show();
                            //verification successful we will start the profile activity

                        }


                    }
                });
    }
void snackBar(int visitorCount){
    final LinearLayout linearLayout =  findViewById(R.id
            .linearLayout);
    Snackbar snackbar= Snackbar.make(linearLayout, "welcome back "+visitorCount+" time.", Snackbar.LENGTH_LONG);
    View snackbarView = snackbar.getView();
    TextView textView=snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
    textView.setTextColor(getResources().getColor(R.color.green));
    textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    textView.setTextSize(16);
    snackbar.setDuration(BaseTransientBottomBar.LENGTH_INDEFINITE);
    snackbar.setActionTextColor(getResources().getColor(R.color.colorBlack));
    snackbar.setAction("OK", new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    });
    snackbar.getView().setBackgroundColor(getResources().getColor(R.color.white));
    snackbar.show();
}



}

