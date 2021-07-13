package com.cjs.hegui30.demo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

/**
 * 合规检测测试程序
 *
 * @author JasonChen
 * @email chenjunsen@outlook.com
 * @createTime 2021/7/13 10:19
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_getIMSI, btn_getMAC, btn_getGPS,btn_getIMEI;
    private TextView tv_display;
    private View container;

    private static final int REQ_CODE_GPS = 101;
    private static final int REQ_CODE_PHONE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.container);

        btn_getIMSI = findViewById(R.id.btn_getIMSI);
        btn_getMAC = findViewById(R.id.btn_getMAC);
        btn_getGPS = findViewById(R.id.btn_getIGPS);
        btn_getIMEI=findViewById(R.id.btn_getIMEI);
        tv_display = findViewById(R.id.tv_display);

        btn_getIMSI.setOnClickListener(this);
        btn_getMAC.setOnClickListener(this);
        btn_getGPS.setOnClickListener(this);
        btn_getIMEI.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btn_getGPS) {
            doGetGps();
        } else if (v == btn_getIMSI) {
            doGetIMSI();
        } else if (v == btn_getMAC) {
            doGetMAC();
        }else if(v==btn_getIMEI){
            doGetIMEI();
        }
    }

    /**
     * 获取DeviceId
     */
    private void doGetIMEI() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQ_CODE_PHONE);
            return;
        }
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String tt="[DeviceId]" + tm.getDeviceId();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            tv_display.setText(tt+"\n"+"[IMEI]"+tm.getImei());
        }
    }

    /**
     * 获取mac地址
     */
    private void doGetMAC() {
        String mac1 = MacUtils.getMacDefault(this);
        String mac2 = MacUtils.getMacFromHardware();
        tv_display.setText("[MAC地址1]" + mac1 + "\n" + "[MAC地址2]"+mac2);
    }

    /**
     * 获取IMSI信息
     */
    private void doGetIMSI() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQ_CODE_PHONE);
            return;
        }
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tv_display.setText("[IMSI]" + tm.getSubscriberId());
    }

    /**
     * 获取GPS信息
     * 注意:经过测试，通过GPS获取定位一般都是空的，只有使用网络获取定位才是有效值
     */
    private void doGetGps() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSOpen = LocationManagerCompat.isLocationEnabled(locationManager);
        if (!isGPSOpen) {
            showSnakeBar("GPS定位没有开启，请开启后重新尝试");
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_CODE_GPS);
                return;
            }
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    Log.d("位置改变", "" + location);
                    tv_display.setText("[位置改变]" + location);
                }
            });
            Log.d("位置", "" + location);
            tv_display.setText("[位置信息]" + location);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_CODE_GPS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    doGetGps();
                } else {
                    showSnakeBar("哦豁！获取GPS被用户拒绝了！！");
                }
                break;
            case REQ_CODE_PHONE:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    doGetIMSI();
                }else{
                    showSnakeBar("哦豁！获取手机状态信息被用户拒绝了！！");
                }
                break;
        }
    }

    /**
     * 在页面底部显示一个snakbar
     *
     * @param text
     */
    private void showSnakeBar(String text) {
        Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
    }
}