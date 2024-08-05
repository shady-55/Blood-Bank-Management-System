package com.example.hospitalbank;

import static com.example.hospitalbank.Fragments.fragment_requests.CONFIRM_DONATION_ID;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.hospitalbank.Classes.Get_Date_Time;
import com.example.hospitalbank.Model.MyDonations;
import com.example.hospitalbank.Model.RequestModel;
import com.example.hospitalbank.Model.User;
import com.example.hospitalbank.Model.UserFaceData;
import com.example.hospitalbank.databinding.ActivityConfirmDonationBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class ConfirmDonationActivity extends AppCompatActivity {
    Toolbar mToolBar;

    FirebaseDatabase database;
    DatabaseReference reference;
    ActivityConfirmDonationBinding binding;
    ProgressDialog progressDialog;
    User user;
    boolean getOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConfirmDonationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        progressDialog = new ProgressDialog(this);
        progressDialog.show();
        init();
        getData();
        mToolBar = findViewById(R.id.Prediction_ToolBar);
        mToolBar.setTitle("Face Data");
        mToolBar.setBackgroundColor(getResources().getColor(R.color.secorange));
        mToolBar.setTitleTextColor(Color.WHITE);
        mToolBar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);
        mToolBar.setNavigationOnClickListener(v -> finish());
        binding.btnConfirmDonationCancel.setOnClickListener(v -> finish());
        binding.btnConfirmDonationConfirm.setOnClickListener(v -> {
            progressDialog.show();
            isStarted=false;
            startDonate(getIntent().getStringExtra(CONFIRM_DONATION_ID));
        });
    }


    private void getData() {
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserFaceData faceData = snapshot.getValue(UserFaceData.class);
                setupViews(faceData);
            }

            private void setupViews(UserFaceData faceData) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(faceData.getAddedAtMillis());
                binding.faceDataName.setText(faceData.getName());
                binding.faceDataAddedAt.setText(formatToDateTime(calendar));
                Bitmap bitmap = decodeBase64ToBitmap(faceData.getFaceImage());
                binding.faceDataImage.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String formatToDateTime(Calendar calendar) {
        return calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DAY_OF_MONTH) + " " + calendar.get(Calendar.HOUR) + ":" + calendar.get(Calendar.MINUTE);
    }

    private void init() {
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("Faces_Data").child(getIntent().getStringExtra(CONFIRM_DONATION_ID));
    }


    public static Bitmap decodeBase64ToBitmap(String base64String) {
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private void startDonate(String userId) {
        checkOnLastDonation(userId);
    }

boolean isStarted =false;
    void checkOnLastDonation(String userId) {
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mReference = mDatabase.getReference("Users/" + userId);
        mReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isStarted){
                    return;
                }
                if (snapshot.getValue() != null) {
                    isStarted=true;
                    User mUser = snapshot.getValue(User.class);
                    user = mUser;
                    String day = mUser.getLastDonationDay().trim();
                    String month = mUser.getLastDonationMonth().trim();
                    String year = mUser.getLastDonationYear().trim();
                    if (day.equals("none")) {
                        startConfirmDonation(userId);
                        return;

                    }
                    if (day.length() == 1) {
                        day = "0" + day;
                    }
                    if (month.length() == 1) {
                        month = "0" + month;
                    }

                    LocalDate date = LocalDate.now();
                    LocalDate date1 = LocalDate.parse(year + "-" + month + "-" + day);


                    Long days = ChronoUnit.DAYS.between(date1, date);
                    Log.d("MYTAG", "onDataChange: " + days);

                    if (days < 90) {
                        progressDialog.dismiss();
                        Toast.makeText(ConfirmDonationActivity.this, "Sorry this user donation hasn't come yet", Toast.LENGTH_SHORT).show();


                    } else {
                        startConfirmDonation(userId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    int recievedAmount;
    boolean changed = false;

    private void startConfirmDonation(String userId) {

        if (changed == false) {
            reference = database.getReference("Request").child("request");
            reference.addValueEventListener(new ValueEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.getValue() == null) {
                        return;
                    }
                    RequestModel myMessage = snapshot.getValue(RequestModel.class);
                    if (myMessage.getBlood_type() == null){
                        progressDialog.dismiss();
                        database.getReference("Request").child("request").removeValue();
                        Toast.makeText(ConfirmDonationActivity.this, "Congratulation you Reached The Target", Toast.LENGTH_SHORT).show();

                        finish();
                        return;
                    }
                    if (!myMessage.getBlood_type().equals(user.getBloodType())){
                        progressDialog.dismiss();
                        Toast.makeText(ConfirmDonationActivity.this, "Sorry current user blood type doesn't match with the request.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int targetedAmount;
                    try {


                        recievedAmount = Integer.parseInt(myMessage.getBlood_recieved());
                        targetedAmount = Integer.parseInt(myMessage.getBlood_amount());
                    } catch (Exception e) {

                        reference = database.getReference("Request").child("request");
                        reference.removeValue();
                        finish();
                        return;
                    }
                    recievedAmount++;
                    if (changed == false) {
                        if (recievedAmount > targetedAmount - 1) {
                            reference = database.getReference("Request").child("request");
                            reference.removeValue();
                            finish();

                        }
                        reference = database.getReference("Request").child("request").child("blood_recieved");
                        reference.setValue(recievedAmount + "");
                        addDonationToUserAccount(userId);
                        updateuserDonationDate(userId);
                        addDonationToHistory();
                        addDonationToDetailedHistory();
                        addToStock();
                        changed = true;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addDonationToDetailedHistory() {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a");
            String time = formatter.format(new Date());
            String date = simpleDateFormat.format(new Date());
            String bloodType = user.getBloodType();
            String email = user.getEmail();
            String userID = user.getUserID();
            String userName = user.getUserName();

            HashMap<String, String> map = new HashMap<>();
            map.put("BloodType", bloodType);
            map.put("Date", date);
            map.put("Email", email);
            map.put("ID", userID);
            map.put("Time", time);
            map.put("Username", userName);
            long millis = System.currentTimeMillis();
            reference = database.getReference("Donations_History").child(String.valueOf(millis));
            reference.setValue(map);
        }


    }

    void addDonationToHistory() {
        String systemMilli = Calendar.getInstance().getTimeInMillis() + "";
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);

        reference = database.getReference("Donations History").child(String.valueOf(year)).child(String.valueOf(month)).child(systemMilli);
        reference.setValue(user.getUserID());
    }

    void updateuserDonationDate(String userId) {

        database = FirebaseDatabase.getInstance();
        database.getReference("Users/" + userId).child("lastDonationDay").setValue(Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "");

        database.getReference("Users/" + userId).child("lastDonationMonth").setValue((Calendar.getInstance().get(Calendar.MONTH) + 1) + "");


        database.getReference("Users/" + userId).child("lastDonationYear").setValue(Calendar.getInstance().get(Calendar.YEAR) + "");


    }

    private void addDonationToUserAccount(String userId) {


        database = FirebaseDatabase.getInstance();
        reference = database.getReference("Donations").child(userId).child(Get_Date_Time.getdate() + System.currentTimeMillis());
        MyDonations myDonations = new MyDonations("1", Get_Date_Time.getdate(), user.getAddress());

        reference.setValue(myDonations);


    }

    void addToStock() {

        String bloodType = user.getBloodType();

        reference = database.getReference("Stock").child(bloodType).child("Number In Stock");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String bags;
                if (snapshot.getValue() == null) {
                    bags = "0";

                } else {
                    bags = (String) snapshot.getValue();
                }
                int newCount = Integer.valueOf(bags) + 1;
                if (!getOnce) {
                    database.getReference("Stock").child(bloodType).child("Number In Stock").setValue(newCount + "");

                    getOnce = true;
                }
                progressDialog.dismiss();
                finish();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }

        });


    }

}
