package com.example.bookapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.barteksc.pdfviewer.PDFView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PdfDetailActivity extends AppCompatActivity {

    //views
    ImageButton backBtn;
    Button readBookBtn;
    PDFView pdfView;
    ProgressBar progressBar;
    TextView titleTv,categoryLabelTv,categoryTv,dateLabelTv,dateTv,sizeLabelTv,sizeTv,viewsLabelTv,
            viewsTv,downloadsLabelTv,downloadsTv,descriptionTv;


    //pdf id, get from intent
    String bookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_detail);

        //init views
        backBtn = (ImageButton) findViewById(R.id.backBtn);
        pdfView = (PDFView) findViewById(R.id.pdfView);
        progressBar =(ProgressBar) findViewById(R.id.progressBar);
        titleTv =(TextView) findViewById(R.id.titleTv);
        categoryLabelTv =(TextView) findViewById(R.id.categoryLabelTv);
        categoryTv =(TextView) findViewById(R.id.categoryTv);
        dateLabelTv =(TextView) findViewById(R.id.dateLabelTv);
        dateTv =(TextView) findViewById(R.id.dateTv);
        sizeLabelTv =(TextView) findViewById(R.id.sizeLabelTv);
        sizeTv =(TextView) findViewById(R.id.sizeTv);
        viewsLabelTv =(TextView) findViewById(R.id.viewsLabelTv);
        viewsTv =(TextView) findViewById(R.id.viewsTv);
        downloadsLabelTv =(TextView) findViewById(R.id.downloadsLabelTv);
        downloadsTv =(TextView) findViewById(R.id.downloadsTv);
        descriptionTv =(TextView) findViewById(R.id.descriptionTv);
        readBookBtn =(Button) findViewById(R.id.readBookBtn);

        //get data from intent e.g. bookId
        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");

        loadCategories();
        //increment book view count, whenever this page starts
        MyApplication.incrementBookViewCount(bookId);


        //handle click, goback
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle click, open to view pdf
        readBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(PdfDetailActivity.this,PdfViewActivity.class);
                intent1.putExtra("bookId", bookId);
                startActivity(intent1);

            }
        });

    }

    private void loadCategories() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        String title = ""+snapshot.child("title").getValue();
                        String description = ""+snapshot.child("description").getValue();
                        String categoryId = ""+snapshot.child("categoryId").getValue();
                        String viewsCount = ""+snapshot.child("viewsCount").getValue();
                        String downloadsCount = ""+snapshot.child("downloadsCount").getValue();
                        String url = ""+snapshot.child("url").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();

                        //format date
                        String date = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                        MyApplication.loadCategory(
                                ""+categoryId,
                                categoryTv
                        );
                        MyApplication.loadPdfFromUrlSinglePage(
                                ""+url,
                                ""+title,
                                pdfView,
                                progressBar
                        );
                        MyApplication.loadPdfSize(
                                ""+url,
                                ""+title,
                               sizeTv
                        );

                        //set data
                        titleTv.setText(title);
                        descriptionTv.setText(description);
                        viewsTv.setText(viewsCount.replace("null","N/A"));
                        downloadsTv.setText(downloadsCount.replace("null","N/A"));
                        dateTv.setText(date);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}
