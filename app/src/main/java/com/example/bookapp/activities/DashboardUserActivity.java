package com.example.bookapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.bookapp.databinding.ActivityDashboardUserBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DashboardUserActivity extends AppCompatActivity {

    //view binding
    private ActivityDashboardUserBinding binding;

    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        CheckUser();

        //handle click logout
        binding.duLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                auth.signOut();
                CheckUser();
            }
        });

    }

    private void CheckUser() {
        //get Current User
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null)
        {
            //not logged in, go to main activity
            startActivity(new Intent(DashboardUserActivity.this,MainActivity.class));
            finish();
        }
        else
        {
            //logged in, get user info
            String email = firebaseUser.getEmail();

            //set on textView of toolbar
            binding.duSubtitleTv .setText(email);
        }
    }
}