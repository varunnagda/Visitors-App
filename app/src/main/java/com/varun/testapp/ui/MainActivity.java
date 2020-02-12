package com.varun.testapp.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.hbb20.CountryCodePicker;
import com.varun.testapp.R;

public class MainActivity extends AppCompatActivity {
    CountryCodePicker ccp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button sumbitButton=findViewById(R.id.submit);
        final EditText personName=findViewById(R.id.personName);
        ccp =findViewById(R.id.ccp);
        Intent intent = getIntent();
         try {

             int response= intent.getIntExtra("response",0);
               if(response>0)
             snackBar("welcome back "+response+" time.");
         } catch (Exception e){
            // snackBar(e.toString());
         }

        try {
            String visitorType= intent.getStringExtra("vtype");
            if(visitorType.equals("authentic_user"))
                snackBar("Successfully registered as authentic user ");
            else if(visitorType.equals("suspicious_user")){
                snackBar("Successfully registered as suspicious user");
            }

        } catch (Exception e){
           // snackBar(e.toString());
        }


        final EditText phoneNumber=findViewById(R.id.phoneNumber);
        sumbitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               final String countryCode=ccp.getSelectedCountryCode();
                Intent intent = new Intent(MainActivity.this, AuthActivity.class);
                String  name= personName.getText().toString();
                String mNumber=phoneNumber.getText().toString();
                int phoneLength=mNumber.length();
                String  number= countryCode+mNumber;
                if(phoneLength==10 && name.length()< 20 && ! name.isEmpty()  && mNumber.matches("[0-9]+")) {
                    intent.putExtra("personName", name);
                    intent.putExtra("phoneNumber", number);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }else if(! (phoneLength==10) || ! (mNumber.matches("[0-9]+")))
                    phoneNumber.setError("Enter valid number.");
                     else personName.setError("Enter valid name.");
            }
        });
    }
    void snackBar(String msg){
        final LinearLayout linearLayout =  findViewById(R.id
                .linearLayoutMain);
        Snackbar snackbar= Snackbar.make(linearLayout, msg, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        TextView textView=snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(getResources().getColor(R.color.green));
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        textView.setTextSize(16);
        snackbar.setDuration(BaseTransientBottomBar.LENGTH_LONG);

        snackbar.getView().setBackgroundColor(getResources().getColor(R.color.white));
        snackbar.show();
    }

}
