package com.example.app2;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_READ_CONTACTS = 1;
    private static boolean READ_CONTACTS_GRANTED = false;
    private static final Set<String> CALL_REQUESTS = new HashSet<>(Arrays.asList("позвони", "сделай вызов", "позвонить", "вызов", "call", "call up"));
    private static final Set<String> OPEN_APP_REQUESTS = new HashSet<>(Arrays.asList("открой", "open"));

    private Map<String, String> contactsMap = new HashMap<>();
    private Map<String, String> appInfoMap = new HashMap<>();
    private TextView textTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initAppInfo();
        initContacts();
        initViews();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case 10:
                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    manageAction(text.get(0));
                    break;
            }
        }
    }

    private void initAppInfo() {
        for (ApplicationInfo appInfo : getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA)) {
            appInfoMap.put(
                    appInfo.loadLabel(getPackageManager()).toString().toLowerCase(),
                    appInfo.packageName
            );
        }
    }

    private void initContacts() {
        int hasReadContactPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        if (hasReadContactPermission == PackageManager.PERMISSION_GRANTED) {
            READ_CONTACTS_GRANTED = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CODE_READ_CONTACTS);
        }
        if (READ_CONTACTS_GRANTED) {
            loadContacts();
        }
    }

    private void loadContacts() {

        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;

        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;

        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(CONTENT_URI, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {

                String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
                String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
                String phoneNumber;
                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));

                if (hasPhoneNumber > 0) {
                    Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null,
                            Phone_CONTACT_ID + " = ?", new String[]{contact_id}, null);

                    while (phoneCursor.moveToNext()) {
                        phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                        contactsMap.put(name.toLowerCase(), phoneNumber);
                    }
                }
            }
        }
    }

    private void initViews() {
        textTest = findViewById(R.id.textTest);
    }

    public void onClickMic(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        startActivityForResult(intent, 10);
    }

    private void manageAction(String action) {
        String command = action.split(" ")[0].toLowerCase();
        if (CALL_REQUESTS.contains(command)) {
            String userName = getCommandValueFromAction(action);

            if (contactsMap.containsKey(userName)) {
                callContact(contactsMap.get(userName));
            } else {
                Toast.makeText(this, "Такого контакта нет", Toast.LENGTH_SHORT).show();
            }
        } else if (OPEN_APP_REQUESTS.contains(command)) {
            String appName = getCommandValueFromAction(action);

            if (appInfoMap.containsKey(appName)) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(appInfoMap.get(appName));
                startActivity(launchIntent);
            } else {
                Toast.makeText(this, "Такого приложения нет", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Для такого действия не найдено команд", Toast.LENGTH_SHORT).show();
        }
    }

    private String getCommandValueFromAction(String action) {
        String[] sepAction = action.split(" ");
        StringBuilder result = new StringBuilder();
        for (int i = 1; i < sepAction.length; i++) {
            result.append(sepAction[i]);
            if (i != sepAction.length - 1)
                result.append(" ");
        }
        return result.toString().toLowerCase();
    }

    private void callContact(String number) {
        String uri = "tel:" + number.trim();
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse(uri));
        startActivity(intent);
    }
}
