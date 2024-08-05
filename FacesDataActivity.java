package com.example.hospitalbank;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hospitalbank.Adapters.FacesDataAdapter;
import com.example.hospitalbank.Model.UserFaceData;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class FacesDataActivity extends AppCompatActivity {
    Toolbar mToolBar;
    RecyclerView mRecyclerView;
    DatabaseReference reference;
    FirebaseDatabase database;
    FirebaseRecyclerOptions<UserFaceData> options;
    FacesDataAdapter facesDataAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faces_data);
        initViews();

        initData();
        mToolBar.setNavigationOnClickListener(v -> finish());
    }

    private void initData() {
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("Faces_Data");

        reference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<UserFaceData> myarraylisy = new ArrayList<>();

                if (snapshot.getValue() != null) {
                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                        UserFaceData faceData = snapshot1.getValue(UserFaceData.class);
                        myarraylisy.add(faceData);
                    }
                    Log.d("TAG", "onDataChange1212: "+myarraylisy.get(0).getName());
                    facesDataAdapter.setData(myarraylisy);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initViews() {
        mRecyclerView = findViewById(R.id.faces_data_rec);
        mToolBar = findViewById(R.id.faces_data_ToolBar);
        mToolBar.setTitle("Faces Data");
        mToolBar.setBackgroundColor(getResources().getColor(R.color.secorange));
        mToolBar.setTitleTextColor(Color.WHITE);
        mToolBar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);
        facesDataAdapter = new FacesDataAdapter(FacesDataActivity.this);
        mRecyclerView.setAdapter(facesDataAdapter);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2, RecyclerView.VERTICAL, false);

        mRecyclerView.setLayoutManager(gridLayoutManager);
    }
}