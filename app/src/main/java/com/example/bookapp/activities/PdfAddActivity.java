package com.example.bookapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.bookapp.databinding.ActivityPdfAddBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfAddActivity extends AppCompatActivity {

    //setup view binding
    private ActivityPdfAddBinding binding;

    //firebase service
    private FirebaseAuth firebaseAuth;

    //array to hold pdf categories
    private ArrayList<String> categoryTitleArrayList,categoryIdArrayList;

    //TAg for debugging
    private static final String TAG = "ADD_PDF_TAG";

    private static final int PDF_PICK_CODE = 1000;

    //Uri of picked pdf
    private Uri pdfUri = null;

    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //init firebase service
        firebaseAuth = FirebaseAuth.getInstance();
        LoadPdfCategories();

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);


        //handle click, go back
        binding.addPdfBackIbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //handle click, attach pdf
        binding.addPdfAttachIbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PdfPickIntent();
            }
        });

        //handle click, pick categories
        binding.addPdfCategoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CategoryPickDialog();
            }
        });

        //handle click, upload pdf
        binding.addPdfSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ValidateDate();
            }
        });
    }

    private String title = "",description = "";
    private void ValidateDate() {
        Log.d(TAG, "ValidateDate: validating data...");

        //get data
        title = binding.addPdfTitleEt.getText().toString().trim();
        description = binding.addPdfDescriptionEt.getText().toString().trim();

        //validate data
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Enter Title...", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Enter Description...", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(selectedCategoryTitle)) {
            Toast.makeText(this, "Pick Category...", Toast.LENGTH_SHORT).show();
        }
        else if (pdfUri == null) {
            Toast.makeText(this, "Pick Pdf...", Toast.LENGTH_SHORT).show();
        }
        else {
            //all data is valid, can upload now
            UploadPdfToStorage();
        }
    }

    private void UploadPdfToStorage() {
        //upload pdf to firebase storage
        Log.d(TAG, "UploadPdfToStorage: uploading pdf to firebase storage...");

        //show progress
        progressDialog.setMessage("Uploading pdf");
        progressDialog.show();

        //timestamp
        long timestamp = System.currentTimeMillis();

        //path of pdf in firebase storage
        String filePathAndName = "Books/" + timestamp;
        //Storage Ref
        StorageReference booksRef = FirebaseStorage.getInstance().getReference(filePathAndName);
        booksRef.putFile(pdfUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "onSuccess: PDF uploaded to storage...");
                        Log.d(TAG, "onSuccess: getting pdf url");

                        //get pdf url
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String uploadPdfUrl = ""+uriTask.getResult();

                        //upload to firebase db
                        UploadPdfInfoToDB(uploadPdfUrl,timestamp);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure: PDF upload failed due to " + e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "PDF upload failed due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void UploadPdfInfoToDB(String uploadPdfUrl, long timestamp) {
        //upload pdf info to firebase Database
        Log.d(TAG, "UploadPdfInfoToDB: uploading pdf to firebase database...");

        progressDialog.setMessage("Uploading Pdf Info...");

        String uid = firebaseAuth.getUid();

        //setup data to upload
        HashMap<String,Object> pdfInfoMap = new HashMap<>();
        pdfInfoMap.put("uid",""+uid);
        pdfInfoMap.put("id",""+timestamp);
        pdfInfoMap.put("title",""+title);
        pdfInfoMap.put("description",""+description);
        pdfInfoMap.put("categoryId",""+selectedCategoryId);
        pdfInfoMap.put("url",""+uploadPdfUrl);
        pdfInfoMap.put("timestamp",""+timestamp);
        pdfInfoMap.put("viewsCount",0);
        pdfInfoMap.put("downloadsCount",0);

        //db reference: db > Books
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(""+timestamp)
                .setValue(pdfInfoMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onSuccess: Successfully uploaded...");
                        Toast.makeText(PdfAddActivity.this, "Successfully uploaded...", Toast.LENGTH_SHORT).show();
                        binding.addPdfTitleEt.setText("");
                        binding.addPdfDescriptionEt.setText("");
                        binding.addPdfCategoryTv.setText("");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure: Failed to upload to db due to "+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "Failed to upload to db due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void LoadPdfCategories() {
        Log.d(TAG, "LoadPdfCategories: Loading Pdf Categories..");

        categoryTitleArrayList = new ArrayList<>();
        categoryIdArrayList = new ArrayList<>();

        //db ref for loading categories ... db>Categories
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance().getReference("Categories");
        categoriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryTitleArrayList.clear(); //clear before adding data
                categoryIdArrayList.clear();

                for (DataSnapshot ds: snapshot.getChildren()) {

                    //get id and title of category
                    String categoryId = ""+ds.child("id").getValue();
                    String categoryTitle = ""+ds.child("category").getValue();

                    //add to respective array lists
                    categoryIdArrayList.add(categoryId);
                    categoryTitleArrayList.add(categoryTitle);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //Select category id and title
    private String selectedCategoryId, selectedCategoryTitle;
    private void CategoryPickDialog() {
        Log.d(TAG, "CategoryPickDialog: showing category pick dialog");

        //get String array of categories from arrayList
        String[] categoriesArray = new String[categoryTitleArrayList.size()];
        for (int i = 0; i< categoryTitleArrayList.size(); i++) {
            categoriesArray[i] = categoryTitleArrayList.get(i);
        }

        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //handle item click
                        //get click item from list
                        selectedCategoryTitle = categoryTitleArrayList.get(i);
                        selectedCategoryId = categoryIdArrayList.get(i);
                        //set to category text view
                        binding.addPdfCategoryTv.setText(selectedCategoryTitle);

                        Log.d(TAG, "onClick: selected category Id ("+selectedCategoryId+") & selected category title ("+selectedCategoryTitle+")");
                    }
                })
                .show();
    }

    private void PdfPickIntent() {
        Log.d(TAG,"pdfPickIntent: starting pdf pick intent");

        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Pdf"), PDF_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PDF_PICK_CODE) {
                Log.d(TAG,"onActivityResult: PDF Picked");

                pdfUri = data.getData();
                Log.d(TAG,"onActivityResult: URI: "+pdfUri);
            }
        }
        else {
            Log.d(TAG, "onActivityResult: cancel picking pdf");
            Toast.makeText(this, "cancel picking pdf", Toast.LENGTH_SHORT).show();
        }
    }
}