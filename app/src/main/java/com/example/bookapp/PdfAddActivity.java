package com.example.bookapp;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

    //views
    ImageButton backBtn,attachBtn;
    ImageView iconIv;
    TextView categoryTv;
    EditText titleEt,descriptionEt;
    Button submitBtn;

    //firebase auth
    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;

    //arraylist to hold pdf categories
    private ArrayList<String> categoryTitleArrayList, categoryIdArrayList;

    //uri of picked pdf
    private Uri pdfUri = null;

    private static final int PDF_PICK_CODE = 1000;

    //TAG for debugging
    private static final String TAG = "ADD_PDF_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_add);

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        loadPdfCategories();

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //init views from xml
        backBtn = (ImageButton) findViewById(R.id.backBtn);
        attachBtn = (ImageButton) findViewById(R.id.attachBtn);
        iconIv = (ImageView) findViewById(R.id.iconIv);
        categoryTv = (TextView) findViewById(R.id.categoryTv);
        titleEt = (EditText) findViewById(R.id.titleEt);
        descriptionEt = (EditText) findViewById(R.id.descriptionEt);
        submitBtn = (Button) findViewById(R.id.submitBtn);

        //handle click, go to previous activity
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle click, attach pdf
        attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pdfPickIntent();
            }
        });

        //handle click, pick category
        categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                categoryPickDialog();
            }
        });

        //handle click, upload pdf
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //validate data
                validateData();
            }
        });

    }

    private String title= "", description = "";

    private void validateData() {
        //step 1: Validate data
        Log.d(TAG, "validateData: Validating data");

        //get data
        title = titleEt.getText().toString().trim();
        description = descriptionEt.getText().toString().trim();

        //validate data
        if (TextUtils.isEmpty(title)){
            Toast.makeText(this, "Please enter book title", Toast.LENGTH_SHORT).show();
        }
        else  if (TextUtils.isEmpty(description)){
            Toast.makeText(this, "Please enter book description", Toast.LENGTH_SHORT).show();
        }
        else  if (TextUtils.isEmpty(selectedCategoryTitle)){
            Toast.makeText(this, "Please select book category", Toast.LENGTH_SHORT).show();
        }
        else if (pdfUri==null){
            Toast.makeText(this, "Please select Pdf", Toast.LENGTH_SHORT).show();
        }
        else {
            //all data is valid, can uploaqd now
            uploadPdfToStorage();
        }
    }

    private void uploadPdfToStorage() {
        //step 2: Upload Pdf to firebase storage
        Log.d(TAG, "uploadPdfToStorage: Uploading Pdf");

        //show progress
        progressDialog.setMessage("Uploading Pdf,wait a moment...");
        progressDialog.show();

        //timestamp
        long timestamp = System.currentTimeMillis();

        //path of pdf in firebase storage
        String filePathAndName = "Books/" + timestamp;
        //storage reference
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
        storageReference.putFile(pdfUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "onSuccess: Pdf uploaded successfully");
                        Log.d(TAG, "onSuccess: getting pdf url");

                        //get pdf url
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String uploadedPdfUrl = ""+uriTask.getResult();

                        //upload to firebase db
                        uploadPdfInfoToDb(uploadedPdfUrl, timestamp);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure: Pdf upload failed due to "+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "Pdf upload failed due to "+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

    }

    private void uploadPdfInfoToDb(String uploadedPdfUrl, long timestamp) {
        //step 3: Upload Pdf Info to firebase db
        Log.d(TAG, "uploadPdfToStorage: Uploading Pdf Information to firebase db");

        progressDialog.setMessage("Uploading pdf Information,Please wait");

        String uid = firebaseAuth.getUid();

        //setup data to upload
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", ""+uid);
        hashMap.put("id", ""+timestamp);
        hashMap.put("title", ""+title);
        hashMap.put("description", ""+description);
        hashMap.put("categoryId", ""+selectedCategoryId);
        hashMap.put("url", ""+uploadedPdfUrl);
        hashMap.put("timestamp", timestamp);
        hashMap.put("viewsCount", 0);
        hashMap.put("downloadsCount", 0);

        //db reference: DB > Books
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(""+timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onSuccess: Pdf Uploaded successfully");
                        Toast.makeText(PdfAddActivity.this, "Pdf Uploaded successfully", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure: Failed to upload due to "+e.getMessage());
                        Toast.makeText(PdfAddActivity.this, " Failed to upload due to "+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

    }

    private void loadPdfCategories() {
        Log.d(TAG, "loadPdfCategories: Loading pdf categories...");
        categoryTitleArrayList = new ArrayList<>();
        categoryIdArrayList = new ArrayList<>();

        //db reference to load categories... db > Categories
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Categories");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryTitleArrayList.clear(); //clear before adding data
                categoryIdArrayList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){

                    //get id and title of category
                   String categoryId = ""+ds.child("id").getValue();
                   String categoryTitle = ""+ds.child("category").getValue();

                   //add to respective arraylists
                    categoryTitleArrayList.add(categoryTitle);
                    categoryIdArrayList.add(categoryId);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    //selected category id and category title
    private String selectedCategoryId, selectedCategoryTitle;


    private void categoryPickDialog() {
        Log.d(TAG, "categoryPickDialog: Showing category pick dialog");

        //get string array of categories from arraylist
        String[] categoriesArray = new String[categoryTitleArrayList.size()];
        for (int i = 0; i< categoryTitleArrayList.size(); i++){
            categoriesArray[i] = categoryTitleArrayList.get(i);
        }

        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Book Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //handle item click
                        //get clicked item from list
                       selectedCategoryTitle = categoryTitleArrayList.get(which);
                       selectedCategoryId = categoryIdArrayList.get(which);
                        //set to category textview
                        categoryTv.setText(selectedCategoryTitle);

                        Log.d(TAG, "onClick: Book Category selected: "+selectedCategoryId+" "+selectedCategoryTitle);

                    }
                })
                .show();

    }

    private void pdfPickIntent() {
        Log.d(TAG, "pdfPickIntent: Picking Pdf");

        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select your PDF"), PDF_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            if (requestCode == PDF_PICK_CODE){
                Log.d(TAG, "onActivityResult: Pdf picked");

                pdfUri = data.getData();

                Log.d(TAG, "onActivityResult: URI: "+pdfUri);

            }
        }else {
            Log.d(TAG, "onActivityResult: Picking Pdf Cancelled");
            Toast.makeText(this, "Picking Pdf Cancelled", Toast.LENGTH_SHORT).show();
        }

    }
}
