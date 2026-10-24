package com.oldschoolnetwork.core;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class ProfilesActivity extends AppCompatActivity {

    private EditText etEmail, etName, etSurname, etUsername, etPhone;
    private TextView tvOnId;
    private ImageView profileAvatar;
    private Spinner spinnerCountry;

    private String currentAvatarBase64 = "";
    private EditText newPassword;
    private BottomSheetDialog avatarDialog;

    private ONAccount onAccount;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAPTURE_IMAGE_REQUEST = 2;
    private static final int CROP_IMAGE_REQUEST = 999;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private Uri cameraImageUri;

    // Списки кодов стран
    private final String[] countryNames = {"🇲🇩 +373", "🇷🇺/🇰🇿 +7", "🇺🇦 +380", "🇧🇾 +375", "🇦🇿 +994", "🇺🇿 +998", "🇺🇸 +1"};
    private final String[] countryCodes = {"+373", "+7", "+380", "+375", "+994", "+998", "+1"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);

        onAccount = new ONAccount(this);

        tvOnId = findViewById(R.id.tv_on_id);
        etEmail = findViewById(R.id.et_email);
        etUsername = findViewById(R.id.et_username);
        etName = findViewById(R.id.et_name);
        etSurname = findViewById(R.id.et_surname);
        etPhone = findViewById(R.id.et_phone);
        profileAvatar = findViewById(R.id.profile_avatar);

        spinnerCountry = findViewById(R.id.spinner_country);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, countryNames);
        spinnerCountry.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_change_avatar).setOnClickListener(v -> showAvatarDialog());
        findViewById(R.id.btn_save).setOnClickListener(v -> saveProfileData());

        newPassword=findViewById(R.id.profileNewPassword);
        loadProfileData();
    }

    private void loadProfileData() {
        try {
            tvOnId.setText(onAccount.getOnId());
            etEmail.setText(onAccount.getEmail());
            etName.setText(onAccount.getName());
            etSurname.setText(onAccount.getSurname());

            String rawUsername = onAccount.getUsername();
            if (rawUsername != null) {
                etUsername.setText(rawUsername.replace("@", ""));
            }

            // Умная подгрузка номера с кодом страны
            String phone = onAccount.getPhoneNumber();
            if (phone != null && !phone.isEmpty()) {
                boolean matched = false;
                for (int i = 0; i < countryCodes.length; i++) {
                    if (phone.startsWith(countryCodes[i])) {
                        spinnerCountry.setSelection(i);
                        etPhone.setText(phone.substring(countryCodes[i].length()));
                        matched = true;
                        break;
                    }
                }
                if (!matched) etPhone.setText(phone);
            }

            String avatarData = onAccount.getAvatar();
            if (avatarData != null && !avatarData.isEmpty()) {
                byte[] decodedString = Base64.decode(avatarData, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                profileAvatar.setImageBitmap(decodedByte);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveProfileData() {
        String email=etEmail.getText().toString().trim(),name=etName.getText().toString().trim(),surname=etSurname.getText().toString().trim(),username=etUsername.getText().toString().trim().replace("@", "");
        String rawPhone=etPhone.getText().toString().trim();String phone=rawPhone.isEmpty()?"":countryCodes[spinnerCountry.getSelectedItemPosition()]+rawPhone;
        if(email.isEmpty()||name.isEmpty()||username.isEmpty()){Toast.makeText(this,"Name, username and email are required.",Toast.LENGTH_LONG).show();return;}
        String password=newPassword.getText().toString();if(!password.isEmpty()&&password.length()<6){newPassword.setError("Use at least 6 characters");return;}
        JSONObject request=new JSONObject();try{request.put("new_password",password);request.put("email",email);request.put("name",name);request.put("surname",surname);request.put("username",username);request.put("phone",phone);}catch(Exception ignored){}
        findViewById(R.id.btn_save).setEnabled(false);final String avatar=currentAvatarBase64;
        AccountApi.requestAsync(this,"account_update_profile",request,(result,error)->{
            if(isFinishing())return;
            if(error!=null){findViewById(R.id.btn_save).setEnabled(true);Toast.makeText(this,error,Toast.LENGTH_LONG).show();return;}
            JSONObject user=result.optJSONObject("user");
            onAccount.updateProfileInfo(user!=null?user.optString("email",email):email,user!=null?user.optString("name",name):name,user!=null?user.optString("surname",surname):surname,user!=null?user.optString("username",username):username,user!=null?user.optString("phone",phone):phone);
            if(!password.isEmpty()){
                android.accounts.AccountManager manager=android.accounts.AccountManager.get(this);
                for(android.accounts.Account account:manager.getAccountsByType(ONAccount.ACCOUNT_TYPE))if(onAccount.getOnId().equals(manager.getUserData(account,"on_id"))||email.equalsIgnoreCase(SystemAccounts.privateLogin(this,account)))manager.setPassword(account,password);
            }
            SystemAccounts.restoreAsync(this);
            if(avatar.isEmpty()){Toast.makeText(this,"Profile saved.",Toast.LENGTH_SHORT).show();finish();return;}
            JSONObject picture=new JSONObject();try{picture.put("avatar_base64",avatar);}catch(Exception ignored){}
            AccountApi.requestAsync(this,"account_update_avatar",picture,(response,avatarError)->{
                if(isFinishing())return;findViewById(R.id.btn_save).setEnabled(true);
                if(avatarError!=null){Toast.makeText(this,"Profile saved. Avatar upload failed: "+avatarError,Toast.LENGTH_LONG).show();return;}
                onAccount.setAvatar(avatar);SystemAccounts.restoreAsync(this);Toast.makeText(this,"Profile saved.",Toast.LENGTH_SHORT).show();finish();
            });
        });
    }

    private void showAvatarDialog() {
        avatarDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_select_avatar, null);

        View btnGallery = view.findViewById(R.id.btn_gallery);
        if (btnGallery != null) {
            btnGallery.setOnClickListener(v -> {
                avatarDialog.dismiss();
                openGallery();
            });
        }

        View btnTakePhoto = view.findViewById(R.id.btn_take_photo);
        if (btnTakePhoto != null) {
            btnTakePhoto.setOnClickListener(v -> {
                avatarDialog.dismiss();
                checkPermissionsAndOpenCamera();
            });
        }

        avatarDialog.setContentView(view);
        try {
            View parent = (View) view.getParent();
            if (parent != null) parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        } catch (Exception e) { e.printStackTrace(); }
        avatarDialog.show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void checkPermissionsAndOpenCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                return;
            }
        }
        openCamera();
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Avatar");
        cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        startActivityForResult(intent, CAPTURE_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            startCropActivity(data.getData());
        }

        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (cameraImageUri != null) startCropActivity(cameraImageUri);
        }

        if (requestCode == CROP_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            String base64 = data.getStringExtra("cropped_base64");
            if (base64 != null) {
                currentAvatarBase64 = base64;
                byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                profileAvatar.setImageBitmap(decodedByte);
            }
        }
    }

    private void startCropActivity(Uri imageUri) {
        Intent cropIntent = new Intent(this, CropActivity.class);
        cropIntent.putExtra("image_uri", imageUri.toString());
        startActivityForResult(cropIntent, CROP_IMAGE_REQUEST);
    }


}