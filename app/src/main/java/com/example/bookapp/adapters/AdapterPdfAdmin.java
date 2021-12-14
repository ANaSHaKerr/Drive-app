package com.example.bookapp.adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.MyApplication;
import com.example.bookapp.activities.PdfDetailActivity;
import com.example.bookapp.activities.PdfEditActivity;
import com.example.bookapp.databinding.RowPdfAdminBinding;
import com.example.bookapp.filters.FilterPdfAdmin;
import com.example.bookapp.models.ModelPdf;
import com.github.barteksc.pdfviewer.PDFView;

import java.util.ArrayList;

public class AdapterPdfAdmin extends RecyclerView.Adapter<AdapterPdfAdmin.HolderPdfAdmin>{

    //view binding
    private RowPdfAdminBinding binding;

    //context
    private final Context context;
    //arrayList to hold list of data of type ModelPdf
    public ArrayList<ModelPdf> pdfArrayList, filterList;


    //progress
    private ProgressDialog progressDialog;

    //constructor
    private FilterPdfAdmin filter ;

    private static final String TAG = "PDF_ADAPTER_TAG";

    public AdapterPdfAdmin(Context context, ArrayList<ModelPdf> pdfArrayList) {
        this.context = context;
        this.pdfArrayList = pdfArrayList;
        this.filterList = pdfArrayList;

        //init progress dialog
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);
    }

    @NonNull
    @Override
    public HolderPdfAdmin onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //binding row_pdf_admin.xml
        binding = RowPdfAdminBinding.inflate(LayoutInflater.from(context),parent,false);

        return new HolderPdfAdmin(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderPdfAdmin holder, int position) {
        /*Get Data , Set Data, Handle clicks etc.*/

        //get data
        ModelPdf model = pdfArrayList.get(position);
        String title = model.getTitle();
        String categoryId = model.getCategoryId();
        String pdfId = model.getId();
        String pdfUrl = model.getUrl();
        String description = model.getDescription();
        String timestamp = model.getTimestamp();
        //we need to convert timestamp dd/MM/yyyy format
        String formattedDate = MyApplication.formatTimestamp(timestamp);

        //set data
        holder.titleTv.setText(title);
        holder.descriptionTv.setText(description);
        holder.dateTv.setText(formattedDate);



        //we will
        //load Further details like category,pdf form url,pdf siz  in Separate function
        MyApplication.loadCategory(""+categoryId, holder.categoryTv);
        MyApplication.loadPdfFormUrlSinglePage(""+pdfUrl, ""+title, holder.pdfView,
                holder.progressBar, null);
        MyApplication.loadPdfSize(""+pdfUrl, ""+title, holder.sizeTv);

        //handle click, show dialog with options 1) Edit,2) Delete
        holder.moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moreOptionsDialog(model, holder);
            }
        });


    //handle book/pdf click,open pdf details page,pass pdf/book id to get details of it
        holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(context, PdfDetailActivity.class);
            intent.putExtra("bookId",pdfId);
            context.startActivity(intent);
        }
    });
    }
    private void moreOptionsDialog(ModelPdf model, HolderPdfAdmin holder) {
        String bookId = model.getId();
        String bookUrl = model.getUrl();
        String bookTitle = model.getTitle();

        //options to show in dialog
        String[] options = {"Edit","Delete"};

        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose Options")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        //handle dialog option click
                        if (which==0){
                            //Edit clicked,open  PdfEditActivity to edit the book info
                            Intent intent = new Intent(context, PdfEditActivity.class);
                            intent.putExtra("bookId", bookId);
                            context.startActivity(intent);
                        }
                        else if (which==1){
                            //Delete clicked
                            MyApplication.deleteBook(
                                    context,
                                    ""+bookId ,
                                    ""+bookUrl,
                                    ""+bookTitle
                            );
                        }

                    }
                })
                .show();

    }

    @Override
    public int getItemCount() {
        return pdfArrayList.size();
    }

    public Filter getFilter() {
        if(filter==null){
            filter = new FilterPdfAdmin(filterList,this);
        }
        return filter;
    }


    /*view holder class to hold UI views for row_pdf_admin.xml*/
    class HolderPdfAdmin extends RecyclerView.ViewHolder{
        //UI views for row_pdf_admin.xml
        PDFView pdfView;
        ProgressBar progressBar;
        TextView titleTv, descriptionTv, categoryTv, sizeTv, dateTv;
        ImageButton moreBtn;

        public HolderPdfAdmin(@NonNull View itemView) {
            super(itemView);

            //init ui views
            pdfView = binding.rowPdfAdminPDFView;
            progressBar = binding.PDFViewProgressbar;
            titleTv = binding.rowPdfTitleTv;
            descriptionTv = binding.rowPdfDescriptionTv;
            categoryTv = binding.rowPdfCategoryTv;
            sizeTv = binding.rowPdfSizeTv;
            dateTv = binding.rowPdfDateTv;
            moreBtn = binding.rowPdfMoreIBtn;

        }
    }
}
