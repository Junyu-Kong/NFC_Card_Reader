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
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msgs[] = null;
            int contentSize = 0;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                    contentSize += msgs[i].toByteArray().length;
                }
            }
            try {
                if (msgs != null) {
                    NdefRecord record = msgs[0].getRecords()[0];
                    String textRecord = parseTextRecord(record);
                    txt2.setText(textRecord + "\n\ntext\n" + contentSize + " bytes");
                }
            } catch (Exception e) {
            }
        }
    }

    public static String parseTextRecord(NdefRecord ndefRecord) {
        /**
         * 判断数据是否为NDEF格式
         */
        //判断TNF
        if (ndefRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
            return null;
        }
        //判断可变的长度的类型
        if (!Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
            return null;
        }
        try {
            //获得字节数组，然后进行分析
            byte[] payload = ndefRecord.getPayload();
            //下面开始NDEF文本数据第一个字节，状态字节
            //判断文本是基于UTF-8还是UTF-16的，取第一个字节"位与"上16进制的80，16进制的80也就是最高位是1，
            //其他位都是0，所以进行"位与"运算后就会保留最高位
            String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
            //3f最高两位是0，第六位是1，所以进行"位与"运算后获得第六位
            int languageCodeLength = payload[0] & 0x3f;
            //下面开始NDEF文本数据第二个字节，语言编码
            //获得语言编码
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            //下面开始NDEF文本数据后面的字节，解析出文本
            String textRecord = new String(payload, languageCodeLength + 1,
                    payload.length - languageCodeLength - 1, textEncoding);
            return textRecord;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }
}
