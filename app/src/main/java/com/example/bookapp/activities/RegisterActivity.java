package com.example.bookapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.bookapp.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    //view binding
    private ActivityRegisterBinding binding;

    private String name ="", email="", password="", confirmPass="";

    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        //Setup progress dialog
        progressDialog = new ProgressDialog(RegisterActivity.this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle click, go back
        binding.registerBackIbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //handle click, begin Register
        binding.registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ValidateData();
            }
        });

    }


    private void ValidateData() {

        //get Data
        name = binding.registerNameEt.getText().toString().trim();
        email = binding.registerEmailEt.getText().toString().trim();
        password = binding.registerPasswordEt.getText().toString().trim();
        confirmPass = binding.registerConfirmPasswordEt.getText().toString().trim();

        //validate data
        if (TextUtils.isEmpty(name))
        {// name edit text is empty, must enter name
            Toast.makeText(this, "Enter your name...!", Toast.LENGTH_SHORT).show();
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {// email is either no entered or email pattern is invalid, don't allow to continue in that case
            Toast.makeText(this, "Invalid email Pattern...!", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(password))
        {// password edit text is empty, must enter password
            Toast.makeText(this, "Enter password...!", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(confirmPass))
        {// confirm password edit text is empty, must enter confirm password

            Toast.makeText(this, "Confirm password...!", Toast.LENGTH_SHORT).show();
        }
        else if (!password.equals(confirmPass))
        {// password and confirm password doesn't match, don't allow to continue in that case
            Toast.makeText(this, "Password doesn't match...!", Toast.LENGTH_SHORT).show();
        }
        else
        {// all data is validated, begin creating account
            CreateUserAccount();
        }

    }

    private void CreateUserAccount() {
        //show progress
        progressDialog.setMessage("Creating account...");
        progressDialog.show();

        //create user in firebase auth
        firebaseAuth.createUserWithEmailAndPassword(email,password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        //account creation success, now add in firebase realtime database
                        UpdateUserInfo();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        //account creation failed
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

    }

    private void UpdateUserInfo() {
        progressDialog.setMessage("Saving user info...");

        //timestamp
        long timestamp = System.currentTimeMillis();

        //get current user id
        String uid = firebaseAuth.getUid();

        //setup data to add in DB
        HashMap<String,Object> userInfoMap = new HashMap<>();
        userInfoMap.put("uid",uid);
        userInfoMap.put("email",email);
        userInfoMap.put("name",name);
        userInfoMap.put("profileImage","");// will do i later
        userInfoMap.put("userType","user");//possible values are user, admin: will make admin manually in firebase realtime db by changing this value
        userInfoMap.put("timestamp",timestamp);

        //set data to db
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(uid)
                .setValue(userInfoMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //data added to db
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Account created...", Toast.LENGTH_SHORT).show();

                        //since user account is created so start dashboard of user
                        startActivity(new Intent(RegisterActivity.this, DashboardUserActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        // data failed adding to db
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }
}