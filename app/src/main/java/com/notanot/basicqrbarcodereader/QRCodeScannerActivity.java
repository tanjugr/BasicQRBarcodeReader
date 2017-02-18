package com.notanot.basicqrbarcodereader;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.net.URL;
import java.util.List;
import java.util.Locale;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static android.Manifest.permission.CAMERA;

public class QRCodeScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private static final int REQUEST_CAMERA=1;
    private ZXingScannerView scannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scannerView = new ZXingScannerView(this);
        setContentView(scannerView);
        int currApiVer = Build.VERSION.SDK_INT;
        if (currApiVer>=Build.VERSION_CODES.M){
            if (checkPerm()){
                Toast.makeText(getApplicationContext(), "Kamera erişim izni verildi", Toast.LENGTH_LONG).show();
            }else{
                requestPerm();
            }
        }
    }

    private boolean checkPerm(){
        return (ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA) == PackageManager.PERMISSION_GRANTED);
    }
    private void requestPerm(){
        ActivityCompat.requestPermissions(this, new String[]{CAMERA}, REQUEST_CAMERA);
    }

    public void onRequestPermResult(int requestCode, String permissions[], int[]grantResults){
        switch (requestCode){
            case REQUEST_CAMERA:
                if (grantResults.length>0) {

                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted) {
                        Toast.makeText(getApplicationContext(), R.string.kameraErisimOk, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.kameraErisimRet, Toast.LENGTH_LONG).show();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(CAMERA)) {
                                showMessageOKCancel("Gerekli izinleri vermelisiniz",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{CAMERA}, REQUEST_CAMERA);
                                                }
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new android.support.v7.app.AlertDialog.Builder(QRCodeScannerActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.M) {
            if (checkPerm()) {
                if(scannerView == null) {
                    scannerView = new ZXingScannerView(this);
                    setContentView(scannerView);
                }
                scannerView.setResultHandler(this);
                scannerView.startCamera();
            } else {
                requestPerm();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scannerView.stopCamera();
    }

    @Override
    public void handleResult(final Result rawResult) {
        final String result = rawResult.getText().toLowerCase(Locale.ENGLISH);
        Log.d("QRCodeScanner", rawResult.getText());
        Log.d("QRCodeScanner", rawResult.getBarcodeFormat().toString());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Barkod Tipi: " + rawResult.getBarcodeFormat().toString());
        builder.setPositiveButton(R.string.okbtn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                scannerView.resumeCameraPreview(QRCodeScannerActivity.this);
            }
        });
        if (result.startsWith("http")){
            builder.setNeutralButton(R.string.gitbtn, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(result));
                    PackageManager packageManager = getPackageManager();
                    List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(browserIntent, 0);
                    boolean intentGuvenli = resolveInfos.size()>0;
                    if (intentGuvenli){
                        startActivity(browserIntent);
                    }else{
                        Toast.makeText(QRCodeScannerActivity.this, "Kodu açmak için gerekli uygulamayı yükleyiniz", Toast.LENGTH_SHORT).show();
                        scannerView.resumeCameraPreview(QRCodeScannerActivity.this);
                    }
                }
            });
        }else {
            builder.setNeutralButton(R.string.kopyala, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("text", result);
                    clipboard.setPrimaryClip(clipData);
                    Toast.makeText(QRCodeScannerActivity.this, "Kopyalandı", Toast.LENGTH_LONG).show();
                    scannerView.resumeCameraPreview(QRCodeScannerActivity.this);
                }
            });
        }
        builder.setMessage(rawResult.getText());
        AlertDialog alert1 = builder.create();
        alert1.show();
    }
}
