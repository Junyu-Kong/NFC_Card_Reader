package com.example.test;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    NfcAdapter nfcAdapter;
    TextView text;
    private PendingIntent pi;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private String metaInfo;
    private Boolean auth = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.textView);
        // 获取默认的NFC控制器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pi = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        mFilters = new IntentFilter[]{ndef,};
        mTechLists = new String[][]{{IsoDep.class.getName()}, {NfcA.class.getName()},};
        // KLog.d(" mTechLists", NfcF.class.getName() + mTechLists.length);
        if (nfcAdapter == null) {
            Toast.makeText(this, "手机不支持nfc", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "设置没开NFC", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        /*
        isoDep CPU卡(ISO 14443-4)  NfcA或NfcB
        m1卡 NfcA
         */
        super.onNewIntent(intent);
        Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        //KLog.e("tagFromIntent", "tagFromIntent" + tagFromIntent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            processIntent(intent);//处理响应
        }

    }

    //页面获取焦点
    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pi, null, null);
    }

    //页面失去焦点
    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    //字符序列转换为16进制字符串
    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            System.out.println(buffer);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }

    private String ByteArrayToHexString(byte[] inarray) {
        int i, j, in;
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A",
                "B", "C", "D", "E", "F"};
        String out = "";
        for (j = 0; j < inarray.length; ++j) {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    private byte[] Hex2Bytes(String hexString) {
        byte[] arrB = hexString.getBytes();
        int iLen = arrB.length;
        byte[] arrOut = new byte[iLen / 2];
        String strTmp = null;
        for (int i = 0; i < iLen; i += 2) {
            strTmp = new String(arrB, i, 2);
            arrOut[(i / 2)] = ((byte) Integer.parseInt(strTmp, 16));
        }
        return arrOut;
    }


    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    private void processIntent(Intent intent) {
        //取出封装在intent中的TAG
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String CardId = ByteArrayToHexString(tagFromIntent.getId());
        metaInfo = "卡片ID:" + CardId + "\n";
        // KLog.e(metaInfo);
        boolean auth = false;
        String tagString = tagFromIntent.toString();
        //读取TAG
        if (tagString.contains("MifareClassic")) {
            metaInfo += "MifareClassic\n";
            readMiCard(tagFromIntent);
        } else {
            readIsoDepTag(tagFromIntent);
        }
    }

    private void readIsoDepTag(Tag tagFromIntent) {
        IsoDep isoDep = IsoDep.get(tagFromIntent);
        try {
            if (!isoDep.isConnected()) {
                isoDep.connect();
            }
            byte[] SELECT = {  //APDU查询语句
                    (byte) 0x00, // CLA = 00 (first interindustry command set)
                    (byte) 0xA4, // INS = A4 (SELECT)
                    (byte) 0x00, // P1 = 00 (select file by DF name)
                    (byte) 0x0C, // P2 = 0C (first or only file; no FCI)
                    (byte) 0x06, // Lc = 6 (data/AID has 6 bytes)
                    (byte) 0x50, (byte) 0x41, (byte) 0x59, (byte) 0x2E, (byte) 0x53, (byte) 0x5A, (byte) 0x54 // AID 应用表示，用于系统区分nfc卡片和启动对应服务

            };
            byte[] result = isoDep.transceive(SELECT);  //尝试请求一次
            // KLog.d(result[0] + "  " + result[1]);   //基本都是错误返回，因为没有nfc的厂家协议说明，啥都做不了
            isoDep.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readMiCard(Tag tagFromIntent) {
        MifareClassic mfc = MifareClassic.get(tagFromIntent);
        try {
            mfc.connect();
            int type = mfc.getType();//获取TAG的类型
            int sectorCount = mfc.getSectorCount();//获取TAG中包含的扇区数
            String typeS = "";
            switch (type) {
                case MifareClassic.TYPE_CLASSIC:
                    typeS = "TYPE_CLASSIC";
                    break;
                case MifareClassic.TYPE_PLUS:
                    typeS = "TYPE_PLUS";
                    break;
                case MifareClassic.TYPE_PRO:
                    typeS = "TYPE_PRO";
                    break;
                case MifareClassic.TYPE_UNKNOWN:
                    typeS = "TYPE_UNKNOWN";
                    break;
            }
            metaInfo += "\n卡片类型：" + typeS + "\n共" + sectorCount + "个扇区\n共"
                    + mfc.getBlockCount() + "个块\n存储空间: " + mfc.getSize() + "B\n";
            for (int j = 0; j < sectorCount; j++) {
                //Authenticate a sector with key A.
                auth = mfc.authenticateSectorWithKeyA(j,
                        MifareClassic.KEY_DEFAULT);
                if (!auth) {
                    if (mfc.authenticateSectorWithKeyA(j,
                            MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                        auth = mfc.authenticateSectorWithKeyA(j,
                                MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY);
                    }
                    if (mfc.authenticateSectorWithKeyA(j,
                            MifareClassic.KEY_NFC_FORUM)) {
                        auth = mfc.authenticateSectorWithKeyA(j,
                                MifareClassic.KEY_NFC_FORUM);
                    }
                }
                int bCount;
                int bIndex;
                if (auth) {
                    metaInfo += "Sector " + j + ":验证成功\n";
                    // 读取扇区中的块
                    bCount = mfc.getBlockCountInSector(j);
                    bIndex = mfc.sectorToBlock(j);
                    for (int i = 0; i < bCount; i++) {
                        byte[] data = mfc.readBlock(bIndex);
                        metaInfo += "Block " + bIndex + " : "
                                + bytesToHexString(data) + "\n";
                        bIndex++;
                    }
                } else {
                    metaInfo += "Sector " + j + ":验证失败\n";
                }
            }
            text.setText(metaInfo);
            //Toast.makeText(this, metaInfo, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
