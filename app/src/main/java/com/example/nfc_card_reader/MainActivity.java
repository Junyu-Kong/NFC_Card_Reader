package com.example.nfc_card_reader;

import static java.lang.Math.pow;
import android.app.PendingIntent;
import android.nfc.tech.IsoDep;
import android.content.Intent;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.nfc.NdefMessage;
import android.os.Parcelable;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IsoDep isoDep;
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
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            isoDep = IsoDep.get(tag);
            try {
                isoDep.connect();  //这里建立我们应用和IC卡
                if (isoDep.isConnected()){
                    txt2.setText("connected");
                    byte[] command = new byte[]{
                            (byte) 0x00,         // CLA (Class)
                            (byte) 0xB2,         // INS (Instruction) for READ RECORD
                            (byte) 0x00,         // P1 (Parameter 1) for SFI selection
                            (byte) (0x80 | 0x0F), // P2 (Parameter 2) for Record number and SFI
                            (byte) 0x20          // Le (Expected length of data to be read)
                    };
                    // byte[] response = isoDep.transceive(command)
                    byte[] ids = tag.getId();
                    String uid = bytesToHexString(ids, ids.length);
                    txt2.setText(uid);
                }
            } catch (IOException e) {
                Toast.makeText(this, "fail to connect", Toast.LENGTH_SHORT);
            }finally {
                try{
                    isoDep.close();
                }catch (IOException e) {
                    Toast.makeText(this, "fail to connect", Toast.LENGTH_SHORT);
                }
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

    public static String parsePublicInfo(byte[] bytes){
        int id = (int)(bytes[19] + bytes[18] * pow(16, 2) + bytes[17] * pow(16, 4)+ bytes[16] * pow(16, 6));
        String output = "id: " + id + "\n";
        output += "生效日期: " + bcdToString(bytes[20]) + bcdToString(bytes[21]) + bcdToString(bytes[22]) + bcdToString(bytes[23]) + "\n";
        output += "失效日期: " + bcdToString(bytes[24]) + bcdToString(bytes[25]) + bcdToString(bytes[26]) + bcdToString(bytes[27]) + "\n";
        return output;
    }

    public static String bytesToHexString(byte[] bytes, int len) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (bytes == null || bytes.length <= 0) return null;
        if (len <= 0) return "";
        for (int i = 0; i < len; i++) {
            int v = bytes[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append("0");
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
    public static String bcdToString(byte a){
        int val = a;
        return String.valueOf(val / 16) + String.valueOf(val % 16);
    }
}
