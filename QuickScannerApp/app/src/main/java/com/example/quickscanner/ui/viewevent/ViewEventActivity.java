package com.example.quickscanner.ui.viewevent;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quickscanner.R;
import com.example.quickscanner.controller.FirebaseImageController;
import com.example.quickscanner.controller.FirebaseEventController;
import com.example.quickscanner.controller.FirebaseUserController;
import com.example.quickscanner.databinding.ActivityVieweventBinding;
import com.example.quickscanner.model.Event;
import com.example.quickscanner.model.User;
import com.example.quickscanner.ui.addevent.QRCodeDialogFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class ViewEventActivity extends AppCompatActivity {
    String eventID;
    private FirebaseEventController fbEventController;
    private FirebaseImageController fbImageController;
    private FirebaseUserController fbUserController;
    private Event event;
    private ActivityVieweventBinding binding;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVieweventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fbEventController = new FirebaseEventController();
        fbImageController = new FirebaseImageController();

        // Display Back Button
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        // Grab any Intent bundle/parameters
        Bundle inputBundle = getIntent().getExtras();
        if (inputBundle != null) {
            eventID = inputBundle.getString("eventID");
            Log.d("Beans", eventID);
        }
        if (eventID != null) {
            Log.e("halpp", "Event ID is: "+eventID);
        }
        fetchEventData();

        // Get a reference to the announcement button
        FloatingActionButton announcementButton = findViewById(R.id.announcement_button);

        // Set an OnClickListener to the button
        announcementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an EditText for the user to input their announcement
                final EditText input = new EditText(ViewEventActivity.this);

                // Create an AlertDialog
                new AlertDialog.Builder(ViewEventActivity.this)
                    .setTitle("Announcement")
                    .setMessage("What do you wish to announce?")
                    .setView(input)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Get the user's input
                            String announcement = input.getText().toString();

                            // TODO: Handle the announcement (e.g., send it to Firebase)

                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });

        Bitmap qrCodeBitmap = generateQRCode(eventID);


        // Get a reference to the share button
        FloatingActionButton shareButton = findViewById(R.id.share_button);

        // Set an OnClickListener to the button
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Convert the QR code Bitmap to a Uri
                if (event != null) {
                    Bitmap qrCodeBitmap = generateQRCode(eventID);
                    if (qrCodeBitmap != null) {
                        String path = MediaStore.Images.Media.insertImage(getContentResolver(), qrCodeBitmap, "QR Code", null);
                        //Log.d("beans","this is wheere it fails");
                        Uri qrCodeUri = Uri.parse(path);

                        // Create an Intent with ACTION_SEND action
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);

                        // Put the Uri of the image and the text you want to share in the Intent
                        shareIntent.putExtra(Intent.EXTRA_STREAM, qrCodeUri);
                        Log.d("halpp", "event is null? "+ (event==null));
                        shareIntent.putExtra(Intent.EXTRA_TEXT, "Join my event titled " + event.getName() + " using this QR code");

                        shareIntent.setType("image/jpeg");

                        // Start the Intent
                        //java.lang.NullPointerException: Attempt to invoke virtual method
                        //'java.lang.String com.example.quickscanner.model.Event.getName()' on a null object reference
                        startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
                    } else {
                        // Show an error message if QR code generation failed
                        Toast.makeText(ViewEventActivity.this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Show an error message or handle the null event object appropriately
                    Toast.makeText(ViewEventActivity.this, "Event data not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    // Fetches the event data from Firestore
    private void fetchEventData() {
        fbEventController.getEvent(eventID).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Log.d("halpp","great success");

                event = documentSnapshot.toObject(Event.class);

                Log.d("BEANS", "DocumentSnapshot data: " + documentSnapshot.getData());
                if (event != null) {
                    // Set the event data to the UI
                    Log.d("halpp",event.getName());
                    setEventDataToUI();
                }
            }

            private void setEventDataToUI() {

                //use event object to update all the views
                binding.eventTitleText.setText(event.getName());
                binding.eventDescriptionText.setText(event.getDescription());
                binding.locationTextview.setText(event.getLocation());
                //User organizer = fbUserController.getUser(event.getOrganizerID());
                //binding.organiserText.setText(organizer.getUserProfile().getName());
                binding.eventTimeText.setText(event.getTime());
                // Set up click listener for the "Generate QR Code" button
                binding.generateQRbtn.setOnClickListener(v -> showQRCodeDialog());

                // Check if the image path is not null or empty
                if (event != null && event.getImagePath() != null && !event.getImagePath().isEmpty()) {
                    // Download the image from Firebase Storage
                    fbImageController.downloadImage(event.getImagePath())
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful() && task1.getResult() != null) {
                                String url = task1.getResult().toString();
                                Picasso.get().load(url).into(binding.eventImageImage);
                            } else {
                                Log.d("ViewEventActivity", "Image download failed, setting default image");
                                binding.eventImageImage.setImageResource(R.drawable.ic_home_black_24dp);
                            }
                        });
                } else {
                    Log.d("ViewEventActivity", "Image path is null or empty");
                    // Set default image or handle the case accordingly
                    binding.eventImageImage.setImageResource(R.drawable.ic_home_black_24dp);
                }
            }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("ViewEventActivity", "Error fetching event data: " + e.getMessage());
                }
            });
    }

    private void showQRCodeDialog() {
        // Check if the event object is available
        if (event != null) {
            // Create and show the QR code dialog fragment
            QRCodeDialogFragment.newInstance(eventID).show(getSupportFragmentManager(), "QRCodeDialogFragment");
        } else {
            // If the event object is not available, show a toast message
            Toast.makeText(ViewEventActivity.this, "Event data not available", Toast.LENGTH_SHORT).show();
        }
    }

    // Handles The Top Bar menu clicks
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle the Back button press
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    //generates QR code
    private Bitmap generateQRCode(String text) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }
}