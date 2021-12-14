package com.example.bookapp.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.bookapp.MyApplication;
import com.example.bookapp.databinding.ActivityPdfDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PdfDetailActivity extends AppCompatActivity {

    //view binging
    ActivityPdfDetailBinding binding;

    // pdf id get from intent
    String bookId, bookTitle,bookUrl;

    boolean isInMyFavorite = false;
    ActivityResultLauncher<Intent> getpermission;

    private FirebaseAuth firebaseAuth;
    private static final String TAG_Download="DOWNLOAD_TAG";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding =ActivityPdfDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //get data from intent e.g.bookId
        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");
        //at start hide download button,
        binding.downloadBookBtn.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null){
          //  checkIsFavorite();
        }
        getpermission=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode()==MainActivity.RESULT_OK){
                    Toast.makeText(getApplicationContext(),"Permission Given",Toast.LENGTH_LONG).show();
                }
            }
        });
        takePermission();

        loadBookDetails();
        //increment book view count ,whenever this page starts
      //  MyApplication.incrementBookViewCount(bookId);

        //handle click , goBack
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        //handle click, open to view pdf
        binding.readBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1=new Intent(PdfDetailActivity.this, PdfViewActivity.class);
                intent1.putExtra("bookId", bookId);
                startActivity(intent1);
            }
        });
        //handle click, download pdf
        binding.downloadBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG_Download,"onClick: Checking permission");
                    Log.d(TAG_Download,"onClick: Permission already granted, can download book");
                    MyApplication.downloadBook(PdfDetailActivity.this,""+bookId,""+bookTitle,""+bookUrl);


            }
        });

        //handle click, add or remove favorite
        binding.favouriteBtn.setOnClickListener(view -> {
          /*  if(firebaseAuth.getCurrentUser() == null){
                Toast.makeText(PdfDetailActivity.this, "You're not logged in", Toast.LENGTH_SHORT).show();

            }
            else{
                if(isInMyFavorite){
                    //in favorite, remove from favorite
                    MyApplication.removeFromFavourite(PdfDetailActivity.this,bookId);
                }
                else{
                    //not in favorite,add to favorite
                    MyApplication.addToFavourite(PdfDetailActivity.this, bookId);
                }
            }*/
        });
    }
    private void loadBookDetails() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        bookTitle = ""+snapshot.child("title").getValue();
                        String description = ""+snapshot.child("description").getValue();
                        String categoryId = ""+snapshot.child("categoryId").getValue();
                        String viewsCount = ""+snapshot.child("viewsCount").getValue();
                        String downloadsCount = ""+snapshot.child("downloadsCount").getValue();
                        bookUrl = ""+snapshot.child("url").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();
                        //required data is loaded, show download button
                        binding.downloadBookBtn.setVisibility(View.VISIBLE              );

                        //format date
                        String date=MyApplication.formatTimestamp(timestamp);

                        MyApplication.loadCategory(
                                ""+categoryId,
                                binding.categorylTv);
                        MyApplication.loadPdfFormUrlSinglePage(
                                ""+bookUrl
                                ,""+bookTitle
                                , binding.pdfView
                                , binding.progressBar,
                                binding.pagesTv
                        );
                        MyApplication.loadPdfSize(
                                ""+bookUrl,
                                ""+bookTitle,
                                binding.sizeTv
                        );
                        //set data
                        binding.titleTv.setText(bookTitle);
                        binding.descriptionTv.setText(description);
                        binding.viewsTv.setText(viewsCount.replace("null","N/A"));
                        binding.downloadsTv.setText(downloadsCount.replace("null","N/A"));
                        binding.datelTv.setText(date);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    //request permission
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted->{
                if (isGranted){
                    Log.d(TAG_Download,"Permission Granted");
                    MyApplication.downloadBook(this,""+ bookId, ""+bookTitle, ""+bookUrl);

                }
                else {
                    Log.d(TAG_Download,"Permission was denied...:");
                    Toast.makeText(this, "Permission was denied", Toast.LENGTH_SHORT).show();
                }


            });
    public void takePermissions() {

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            try {

                Intent intent=new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                getpermission.launch(intent);
                MyApplication.downloadBook(this,""+ bookId, ""+bookTitle, ""+bookUrl);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }else{

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},101);

        }
    }

    public  boolean isPermissionGranted(){

        if(Build.VERSION.SDK_INT==Build.VERSION_CODES.R){
            return Environment.isExternalStorageManager();
        }else{
            int readExternalStoragePermission= ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return readExternalStoragePermission==PackageManager.PERMISSION_GRANTED;
        }


    }

    public void takePermission(){

        if(isPermissionGranted()){
            Log.d("Permission","Permission Already given");
        }else{
            takePermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0){
            if(requestCode==101){
                boolean readExternalStorage=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                if(readExternalStorage){
                    Log.d("Permissions","Permission allow in android 10 or below");
                }else{
                    takePermissions();
                }
            }
        }

    }
}