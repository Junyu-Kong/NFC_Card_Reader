package com.example.nfc_card_reader;

import static java.security.AccessController.getContext;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.NfcAdapter;
import java.util.Arrays;
import android.os.Bundle;
import android.nfc.NdefMessage;
import android.os.Parcelable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    TextView txt1;
    TextView txt2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txt1 = findViewById(R.id.t1);
        txt2 = findViewById(R.id.t2);

        // 获取默认的 NFC 适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // 确保拥有NFC功能
        if (nfcAdapter == null) {
            txt1.setText("您没有NFC功能");
            return;
        }

        // 确保NFC功能已启用
        if (!nfcAdapter.isEnabled()) {
            txt1.setText("您没有启用NFC功能");
        } else {
            txt1.setText("您已启用NFC功能");
        }
        // 创建一个 PendingIntent，用于处理 NFC 意图
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 启用前台调度，以便处理 NFC 意图
        if (nfcAdapter != null) {
            // 启用前台调度
            nfcAdapter.enableForegroundDispatch(
                    this, pendingIntent, null, null
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 禁用前台调度
        if (nfcAdapter != null) {
            // 禁用前台调度
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        txt2.setText("intent received");

        // 获取消息
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (messages != null){
            txt2.setText("not null");
            for (Parcelable message : messages){
                NdefMessage ndefMessage = (NdefMessage) message;
                for (NdefRecord record: ndefMessage.getRecords()) {
                    switch(record.getTnf()){
                        case NdefRecord.TNF_WELL_KNOWN:
                            txt2.setText("Well Known: ");
                            if (Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)){
                                txt2.append("Text:");
                                txt2.append(new String(record.getPayload()));
                            }
                            else if (Arrays.equals(record.getType(), NdefRecord.RTD_URI)){
                                txt2.append("URI:");
                                txt2.append(new String(record.getPayload()));
                            }
                    }
                }
            }
        }
        else {
            txt2.setText("null");
        }
    }
}
