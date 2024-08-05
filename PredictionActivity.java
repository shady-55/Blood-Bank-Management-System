package com.example.hospitalbank;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hospitalbank.Adapters.PredictionAdapter;
import com.example.hospitalbank.Adapters.PredictionInterface;
import com.example.hospitalbank.Model.Donors;
import com.example.hospitalbank.Model.MyDonations;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class PredictionActivity extends AppCompatActivity {

    RecyclerView donorsRecycler;
    DatabaseReference reference;
    FirebaseDatabase database;
    ArrayList<Donors> donorsArrayList = new ArrayList<>();
    PredictionAdapter predictionAdapter;
    LinearLayoutManager linearLayoutManager;
    TextView txtAmount;
    Button btnPredict;
    OkHttpClient client;

    Toolbar mToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);
        init();

        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                Intent intent = new Intent(PredictionActivity.this, fragment_home.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
                finish();
            }
        });

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                donorsArrayList.clear();
                if (snapshot.exists()) {

                    for (DataSnapshot donors : snapshot.getChildren()) {

                        Donors value = donors.getValue(Donors.class);
                        donorsArrayList.add(value);
                    }


                }
                predictionAdapter = new PredictionAdapter(donorsArrayList, getBaseContext(), new PredictionInterface() {
                    @Override
                    public void onClick(Donors donor) {
                        predictForSingleUser(donor.getUserID());
                    }
                });
                donorsRecycler.setLayoutManager(linearLayoutManager);
                donorsRecycler.setAdapter(predictionAdapter);

                Log.d("TAG50", "onDataChange: " + donorsArrayList.size());
                txtAmount.setText(donorsArrayList.size() + "/" + donorsArrayList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                predictForAllUsers();
            }
        });


    }


    private void predictForAllUsers() {
        client = new OkHttpClient();

        reference = database.getReference("Users");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Donors mUser = dataSnapshot.getValue(Donors.class);

                    String day = mUser.getLastDonationDay().trim();
                    String month = mUser.getLastDonationMonth().trim();
                    String year = mUser.getLastDonationYear().trim();

                    if (day.length() == 1) {
                        day = "0" + day;
                    }
                    if (month.length() == 1) {
                        month = "0" + month;
                    }

                    LocalDate date = LocalDate.now();

                    LocalDate date1 = LocalDate.parse(year + "-" + month + "-" + day);


                    Long months = ChronoUnit.MONTHS.between(date1, date);
                    Log.d("MYTAG", "onDataChange: " + months);

                    DatabaseReference ref2 = FirebaseDatabase.getInstance().getReference("Donations").child(mUser.getUserID());
                    ArrayList<MyDonations> myarraylisy = new ArrayList<>();
                    ref2.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot snapshot1 : snapshot.getChildren()) {

                                MyDonations myDonations = snapshot1.getValue(MyDonations.class);
                                myarraylisy.add(myDonations);


                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                    String lastDonationMonth = mUser.getLastDonationMonth();
                    String numberOfDonation = myarraylisy.size() + "";
                    int totalVoulumeDonated = myarraylisy.size() * 450;
                    long monthsSinceLastDonation = months;

                    Log.d("MYTAG", "onDataChange: " + lastDonationMonth + " num " + numberOfDonation + " vol " + totalVoulumeDonated + " months " + monthsSinceLastDonation);


                    String url = "http://192.168.1.9:5000/predict";

                    RequestBody formBody = new FormBody.Builder()
                            .add("v1", lastDonationMonth)
                            .add("v2", numberOfDonation)
                            .add("v3", String.valueOf(totalVoulumeDonated))
                            .add("v4", String.valueOf(monthsSinceLastDonation))
                            .build();

                    Request request = new Request.Builder().url(url).post(formBody).build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            Log.d("TAGM", "onResponse: " + e.getMessage());
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                            String string = response.body().string();

                            DatabaseReference updateRef = FirebaseDatabase.getInstance().getReference("Users")
                                    .child(mUser.getUserID()).child("prediction_msg");
                            double predictionValue = Double.parseDouble(string);
                            if (predictionValue > 0.8) {
                                updateRef.setValue("Dear ,\n" +
                                        "Based on the analysis of your records, it appears that everything is looking promising. The data suggests that you are in a good position to make accurate predictions. Keep up the great work!");
                            } else {
                                updateRef.setValue("Dear ,\n" +
                                        "After reviewing your records, it seems there might be some challenges to address. The current data suggests that predictions might not be as reliable as desired. Let's work together to refine the analysis and improve the accuracy of future predictions");
                            }
                        }
                    });


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void init() {
        donorsRecycler = findViewById(R.id.prediction_recyclerView);
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("Users");
        linearLayoutManager = new LinearLayoutManager(this);

        txtAmount = findViewById(R.id.txtAmount);
        btnPredict = findViewById(R.id.btn_predict);
        mToolBar = findViewById(R.id.Prediction_ToolBar);
        mToolBar.setTitle("Prediction");
        mToolBar.setBackgroundColor(getResources().getColor(R.color.secorange));
        mToolBar.setTitleTextColor(Color.WHITE);
        mToolBar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);


    }

    private void predictForSingleUser(String Uid) {
        client = new OkHttpClient();

        reference = database.getReference("Users").child(Uid);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Donors mUser = snapshot.getValue(Donors.class);

                    String day = mUser.getLastDonationDay().trim();
                    String month = mUser.getLastDonationMonth().trim();
                    String year = mUser.getLastDonationYear().trim();

                    if (day.length() == 1) {
                        day = "0" + day;
                    }
                    if (month.length() == 1) {
                        month = "0" + month;
                    }

                    LocalDate date = LocalDate.now();

                    LocalDate date1 = LocalDate.parse(year + "-" + month + "-" + day);


                    Long months = ChronoUnit.MONTHS.between(date1, date);
                    Log.d("MYTAG", "onDataChange: " + months);

                    DatabaseReference ref2 = FirebaseDatabase.getInstance().getReference("Donations").child(Uid);
                    ArrayList<MyDonations> myarraylisy = new ArrayList<>();
                    ref2.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            MyDonations myDonations = snapshot.getValue(MyDonations.class);
                            myarraylisy.add(myDonations);


                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                    String lastDonationMonth = mUser.getLastDonationMonth();
                    String numberOfDonation = myarraylisy.size() + "";
                    int totalVoulumeDonated = myarraylisy.size() * 450;
                    long monthsSinceLastDonation = months;

                    Log.d("MYTAG", "onDataChange: " + lastDonationMonth + " num " + numberOfDonation + " vol " + totalVoulumeDonated + " months " + monthsSinceLastDonation);


                    String url = "http://192.168.1.9:5000/predict";

                    RequestBody formBody = new FormBody.Builder()
                            .add("v1", lastDonationMonth)
                            .add("v2", numberOfDonation)
                            .add("v3", String.valueOf(totalVoulumeDonated))
                            .add("v4", String.valueOf(monthsSinceLastDonation))
                            .build();
                    Request request = new Request.Builder().url(url).post(formBody).build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            Log.d("TAGM", "onResponse: " + e.getMessage());
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            String string = response.body().string();

                            DatabaseReference updateRef = FirebaseDatabase.getInstance().getReference("Users")
                                    .child(mUser.getUserID()).child("prediction_msg");
                            double predictionValue = Double.parseDouble(string);
                            if (predictionValue > 0.8) {
                                updateRef.setValue("Dear ,\n" +
                                        "Based on the analysis of your records, it appears that everything is looking promising. The data suggests that you are in a good position to make accurate predictions. Keep up the great work!");
                            } else {
                                updateRef.setValue("Dear ,\n" +
                                        "After reviewing your records, it seems there might be some challenges to address. The current data suggests that predictions might not be as reliable as desired. Let's work together to refine the analysis and improve the accuracy of future predictions");
                            }

                        }
                    });


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


}