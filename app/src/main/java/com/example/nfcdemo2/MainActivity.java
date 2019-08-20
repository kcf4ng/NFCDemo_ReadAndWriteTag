package com.example.nfcdemo2;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    NfcAdapter nfcAdapter;

    public void tglReadWriteOnClick(View view) {
        txtTagContent.setText("");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InitialComponent();

        NFCAvailable();
    }

    private void InitialComponent() {
        tglReadWrite = findViewById(R.id.tglReadWrite);
        txtTagContent = findViewById(R.id.txtTagContent);
    }

    private void NFCAvailable() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter != null && nfcAdapter.isEnabled()){
            Toast.makeText(this,"NFC available :)", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this,"NFC not available :(", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableForegroundDispatchSystem();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatchSystem();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if(intent.hasExtra(NfcAdapter.EXTRA_TAG)){
            Toast.makeText(this,"NFC intent received !",Toast.LENGTH_SHORT).show();
            Log.d(TAG, "NFC intent received !");
        }

        if(tglReadWrite.isChecked())
        {
            Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if(parcelables != null && parcelables.length>0){
                readTextFromMessage((NdefMessage)parcelables[0] );
            }else{
                Log.d(TAG,"No NDEF Messsage found");
            }


        }else
        {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            NdefMessage ndefMessage = createNdefMessage(txtTagContent.getText()+"");
            writeNdefMesssge(tag,ndefMessage);
        }
    }

    private void readTextFromMessage(NdefMessage ndefMessage) {
        NdefRecord[] ndefRecords = ndefMessage.getRecords();

        if(ndefRecords != null && ndefRecords.length>0)
        {
            NdefRecord ndefRecord = ndefRecords[0];
            String tagContent = getTextFromNdefRecord(ndefRecord);
            txtTagContent.setText(tagContent);
        }else{
            Toast.makeText(this,"NoNDEF records found", Toast.LENGTH_SHORT).show();
        }

    }

    private String getTextFromNdefRecord(NdefRecord ndefRecord) {
        String tagContent = null;

        try{
            byte[]  payload = ndefRecord.getPayload();
            String textEnconding = ((payload[0] & 128) == 0) ? "UTF-8":"UTF-16";
            int languageSize = payload[0] & 0063;
            tagContent = new String(payload,languageSize+1,payload.length-languageSize-1,textEnconding);

        }catch (Exception e){
            Log.e(TAG,e.getMessage(),e);
        }

        return tagContent;
    }


    private void enableForegroundDispatchSystem() {

        Intent intent = new Intent (this, MainActivity.class);

        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);

        IntentFilter[] filter = new IntentFilter[]{};

        nfcAdapter.enableForegroundDispatch(this,pendingIntent,filter, null);
    }

    private void disableForegroundDispatchSystem() {

        nfcAdapter.disableForegroundDispatch(this);

    }

    private void formatTag(Tag tag, NdefMessage ndefMessage)  {
        Log.d(TAG,"format Tag");

        try {

            NdefFormatable ndefFormatable = NdefFormatable.get(tag);
            if(ndefFormatable == null )
            {
                Log.d(TAG,"Tag is not ndef formatable!  ");
                Toast.makeText(this, "Tag is not ndef formatable! ", Toast.LENGTH_SHORT).show();
            }

            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();

//            Toast.makeText(this, "Tag  written", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG,e.getMessage());
        }

    }

    private void writeNdefMesssge(Tag tag,NdefMessage ndefMessage)
    {

        try{

            if(tag == null)
            {
                Toast.makeText(this,"Tag object cannot be null", Toast.LENGTH_SHORT).show();
                return;
            }

            Ndef ndef = Ndef.get(tag);

            if(ndef == null) {
                Log.d(TAG,"Ndef is null");
                //format the tag w/ the ndef format and writes the message;
                formatTag(tag,ndefMessage);
            }else {
                Log.d(TAG,"Tag written :Start");

                ndef.connect();

                if(!ndef.isWritable())
                {
                    Toast.makeText(this, "Tag  is not writable!", Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return;
                }


                ndef.writeNdefMessage(ndefMessage);
                ndef.close();
                Toast.makeText(this, "Tag  written", Toast.LENGTH_SHORT).show();
                Log.d(TAG,"Tag written : Done");
            }

        }catch(Exception e){
            Log.e(TAG,e.getMessage());
        }
    }

    private NdefRecord createTextRecord(String content) {
        Log.d(TAG, "create text Record");
        try {
            byte[] language ;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");

            final byte[] text = content.getBytes("UTF-8");
            final int languageSize = language.length;
            final int textLength = text.length;
            final ByteArrayOutputStream payload = new ByteArrayOutputStream(1+languageSize+ textLength);

            payload.write((byte)(languageSize & 0x1F));
            payload.write(language,0, languageSize  );
            payload.write(text, 0, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN,NdefRecord.RTD_TEXT,new byte[0], payload.toByteArray());


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return  null;
    }

    private NdefMessage createNdefMessage(String content){

        Log.d(TAG, "create NdefMessage");

        NdefRecord ndefRecord =createTextRecord(content);

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ ndefRecord});

        return ndefMessage;
    }

    ToggleButton tglReadWrite;
     EditText txtTagContent;


}

