package com.example.bookapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    //views
    Button registerBtn;
    ImageButton backBtn;
    EditText nameEt,passwordEt,emailEt,cPasswordEt;

    //firebase auth
    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();

        //set up progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading, Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //init views
        backBtn = (ImageButton) findViewById(R.id.backBtn);
        registerBtn = (Button) findViewById(R.id.registerBtn);
        nameEt = (EditText) findViewById(R.id.nameEt);
        emailEt = (EditText) findViewById(R.id.emailEt);
        passwordEt = (EditText) findViewById(R.id.passwordEt);
        cPasswordEt = (EditText) findViewById(R.id.cPasswordEt);


        //handle click, go back
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle click, begin register
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });

    }

    private String name = "", email = "", password = "";

    private void validateData() {
        /*Before creating account, let's do some data validation*/

        //get data
        name = nameEt.getText().toString().trim();
        email = emailEt.getText().toString().trim();
        password = passwordEt.getText().toString().trim();
        String cPassword = cPasswordEt.getText().toString().trim();

        //validate data
        if (TextUtils.isEmpty(name)){
            //name edit text is empty, must enter name
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //email is either not entered or email pattern is invalid, don't allow to contain in that case
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(password)){
            //password edit text is empty, must enter password
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(cPassword)){
            //confrim password edit text is empty, must enter confrim password
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show();
        }
        else if (!password.equals(cPassword)){
            //password and confirm password doesn't match, don't allow to continue in that case, both password must match
            Toast.makeText(this, "Please confirm with a matching password", Toast.LENGTH_SHORT).show();
        }
        else {
            //all data is validated, begin creating account
            createUserAccount();
        }
    }

    private void createUserAccount() {
        //show progress
        progressDialog.setMessage("Creating account, Please wait...");
        progressDialog.show();

        //create user in firebase auth
        firebaseAuth.createUserWithEmailAndPassword(email,password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        //account creation success, now add in firebase realtime database
                        updateUserInfo();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //account creating failed
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

    }

    private void updateUserInfo() {
        progressDialog.setMessage("Saving your account, Please wait");

        //timestamp
        long timestamp = System.currentTimeMillis();

        //get current user uid, since user is registered so we can get now
        String uid = firebaseAuth.getUid();

        //set data to add in db
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", uid);
        hashMap.put("email", email);
        hashMap.put("name", name);
        hashMap.put("profileImage", ""); // add empty, will do later
        hashMap.put("userType", "user"); //possible values are user, admin: will make admin manually in firebase realtime database by changing this value
        hashMap.put("timestamp", timestamp);

        //set data to db
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(uid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //data added to db
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Your account was created successfully", Toast.LENGTH_SHORT).show();
                        //since user account is created so start dashboard of user
                        startActivity(new Intent(RegisterActivity.this, DashboardUserActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //data failed adding to db
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }
}
