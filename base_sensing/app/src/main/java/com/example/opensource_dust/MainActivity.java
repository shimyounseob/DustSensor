package com.example.opensource_dust;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.net.wifi.ScanResult;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static final String[] MAC_ADDRS = {

//            // 1조
            "D8:3A:DD:C1:88:E8",
            "D8:3A:DD:C1:89:5B",
            "D8:3A:DD:C1:88:BD",
            "D8:3A:DD:C1:88:D7",
            "D8:3A:DD:79:8E:D9",
            "D8:3A:DD:42:AC:9A",
            "D8:3A:DD:42:AB:FB",
            "D8:3A:DD:79:8E:9B"
    };

    public static final String[] DUST_ADDRS = {
            "D8:3A:DD:79:8E:D9",
            "D8:3A:DD:42:AC:9A",
            "D8:3A:DD:42:AB:FB",
            "D8:3A:DD:79:8E:9B"
    };
    public static final String[] AIR_ADDRS = {
            "D8:3A:DD:C1:88:E8",
            "D8:3A:DD:C1:89:5B",
            "D8:3A:DD:C1:88:BD",
            "D8:3A:DD:C1:88:D7"
    };


    // 기본 주소 URL
    public static final String BASE_URL = "http://203.255.81.72:10021/";  // 학교
    public Button btnScan;
    public Button btnStop;
    public Button btnRegister;
    public TextView txtPassword;
    public TextView txtTime;
    public TextView txtData;
    public TextView txtResult;
    public TextView txtLog;
    private String detectedMacAddress;  // 검색된 MAC 주소를 저장할 멤버 변수
    private Retrofit retrofit2;
    private WifiManager wifiman;
    private BluetoothAdapter blead;
    String wifidata;
    String id;
    String key;
        void start() throws InterruptedException {
        wifidata = "";
        boolean scanStarted = wifiman.startScan();
        if (!scanStarted) {
            txtLog.append("스캔 실패!");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.txtdata), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Android studio 권한 자동 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
        // Android studio 권한 자동 확인(내 폰에서 BLUETOOTH_SCAN 퍼미션 추가)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1000);
        }
        // Bluetooth adapter 객체 생성
        blead = BluetoothAdapter.getDefaultAdapter();
        // Bluetooth adapter를 사용하지 못하는 상태면 사용 가능하게 설정
        if (!blead.isEnabled())
            blead.enable();
        btnScan = (Button) findViewById(R.id.btn_scan);
        wifiman = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiman == null) {
            txtLog.append("\nWifiManager 초기화 실패");
            return;
        }
        // BroadcastReceiver 등록
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(rssiReceiver, filter);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                    blead.startLeScan(scancallback_le);
                    txtLog.append("\n스캔 시작");
                    try {
                        start();
                    } catch (Exception e) {
                        txtLog.append("\n" + e);
                    }
                    txtLog.append("\n" + id);
                }}});

        btnStop = (Button) findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    blead.stopLeScan(scancallback_le);
                    txtLog.append("\n스캔 중지");
                }}});

        Gson gson = new GsonBuilder().setLenient().create();
        retrofit2 = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        txtResult = (TextView) findViewById(R.id.txt_result);
        txtPassword = (TextView) findViewById(R.id.txt_password);
        txtTime = (TextView) findViewById(R.id.txt_time);
        txtData = (TextView) findViewById(R.id.txt_data);
        txtLog = (TextView) findViewById(R.id.txt_log);
        txtLog.setMovementMethod(new ScrollingMovementMethod());
    }
    public BroadcastReceiver rssiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                    boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                    if (success) {
                        scanSuccess();
                    } else {
                        txtLog.append("\nBRoadwifi스캔 실패");
                    }}}
            catch (Exception e) {
                txtLog.append("\nWiFi 스캔 결과 처리 중 예외 발생: " + e.getMessage());
            }}};
    private void scanSuccess() {
        List<ScanResult> scanresult = wifiman.getScanResults();
        for (int i = 0; i < scanresult.size(); i++) {
            int RSSI = scanresult.get(i).level;
            String BSSID = scanresult.get(i).BSSID;
            //System.out.println(RSSI + "\t" + SSID + "\t" + BSSID);
            wifidata += (BSSID + "!" + String.valueOf(RSSI) + "/");
        }
        location_data location_service = retrofit2.create(location_data.class);
        Call<String> call = location_service.sendLocationSensorData(wifidata);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    txtLog.append("\n*********************위치Success*******************: \n\n" + response.body());
                    key=response.body();
                } else {
                    txtLog.append("Error: " + response.errorBody().toString());
                }
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                txtLog.append("\n데이터 전송 실패: " + t.getMessage());
            }
        });
    }

    private boolean checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            return false;
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1000);
            return false;
        } else {
            return true;
        }}
    private BluetoothAdapter.LeScanCallback scancallback_le = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            try {
                String macAdd = device.getAddress();
                String data = byteArrayToHex(scanRecord);
                for (String validMac : MAC_ADDRS) {
                    if (macAdd.equalsIgnoreCase(validMac)) {
                        detectedMacAddress = device.getAddress();  // 검색된 MAC 주소를 멤버 변수에 저장
                        txtLog.append("\n\n\n******* 데이터 확인 *******\n" + macAdd);
                        txtLog.append("\n" + data);
                        Integer IsDust = -1;
                        String sensorData="";


                        for (String DustMac : DUST_ADDRS) {
                            if (detectedMacAddress.equalsIgnoreCase(DustMac)) {
                                sensorData = extractDataAfterPattern(data, "FD", 3, true);
                                txtData.setText(sensorData);
                                txtLog.append("\nSensor Data: " + sensorData);
                                txtLog.append("\nDUST_DATA");
                                IsDust = 1;// dust
                                break;
                            }
                        }
                        for (String AirMac : AIR_ADDRS) {
                            if (detectedMacAddress.equalsIgnoreCase(AirMac)) {
                                sensorData = extractHexAfterPattern_Air(data, "FD0", 3);
                                txtData.setText(sensorData);
                                txtLog.append("\nSensor Data: " + sensorData);
                                txtLog.append("\nAIR_DATA");
                                IsDust=0;//air
                                break;
                            }}

                        String Otp = extractDataAfterPattern(data, "F0F0", 3, false);
                        String sensingTime = extractDataAfterPattern_E(data, "9999", 5, false);
                        txtPassword.setText(Otp);
                        txtTime.setText(sensingTime);

                        // 로그에 추출된 데이터를 출력
                        txtLog.append("\nTime OTP: " + Otp);
                        txtLog.append("\nSensing Time: " + sensingTime);
                        sendSensorData(macAdd, sensorData, IsDust);
                    }
                }
            } catch (Exception e) {
                txtLog.append("\nBLE 스캔 처리 중 예외 발생: " + e.getMessage());
                Log.e("ble", "BLE 스캔 처리 중 예외 발생", e);
            }}};

    private void sendSensorData(String detectedMacAddress, String data, Integer IsDust) {
        try {
            String otpValue = txtPassword.getText().toString(); // OTP 값 추출
            String timeValue = txtTime.getText().toString(); // 시간 값 추출
            String dataValue = txtData.getText().toString(); // 데이터 값 추출
            location_data location_service = retrofit2.create(location_data.class);
            // 서버로 전송할 데이터 구성 및 전송
            if (IsDust == 1&& key!=null) {
                dust_comm_data dust_service = retrofit2.create(dust_comm_data.class);
                Call<String> call = dust_service.sendDustSensorData("3jo", "advertising", detectedMacAddress, id, timeValue, otpValue, key, dataValue);
                txtLog.append("\n위치 정보 : "+key);
                txtLog.append("\n3jo" + "advertising" + detectedMacAddress + id + timeValue + otpValue + key + dataValue);
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        Log.e("받은 데이터",response.body().toString());
                        if (response.isSuccessful()) {
                            txtLog.append("더스트 응답 받기 성공"+response.toString() );
                        } else {
                            txtResult.setText("Error: " + response.errorBody().toString());
                        }
                    }
                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        txtResult.setText("Failed to send data: " + t.getMessage());
                    }
                });
            }
            if (IsDust == 0&&key!=null) {
                air_comm_data air_service = retrofit2.create(air_comm_data.class);
                Call<String> call = air_service.sendAirSensorData("3jo", "advertising", detectedMacAddress, id, timeValue, otpValue, key, dataValue);
                txtLog.append("\n위치 정보 : "+key);
                txtLog.append("\n3jo" + "advertising" + detectedMacAddress + id + timeValue + otpValue + key + dataValue);
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful()) {
                            txtLog.append("에어 응답 받기 성공 "+response.toString());
                        } else {
                            txtResult.setText("Error: " + response.errorBody().toString());
                        }
                    }
                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        txtResult.setText("Failed to send data: " + t.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            txtLog.append("\n센서 데이터 전송 중 예외 발생: " + e.getMessage());
        }}

    public String extractDataAfterPattern(String data, String pattern, int byteCount, boolean addSlashBetweenBytes) {
        StringBuilder sb = new StringBuilder();
        String noSpaceData = data.replace(" ", "");
        int index = noSpaceData.indexOf(pattern);
        if (index != -1) {
            index += pattern.length();
            for (int i = 0; i < byteCount; i++) {
                if (addSlashBetweenBytes && i > 0) sb.append("/");  // 필요 시 '/'로 구분
                int byteIndex = index + i * 2;
                int byteValue = Integer.parseInt(noSpaceData.substring(byteIndex, byteIndex + 2), 16);
                sb.append(byteValue);  // 변환된 값을 StringBuilder에 추가
            }
        }
        return sb.toString();
    }

    public String extractHexAfterPattern_Air(String data, String pattern, int charCount) {
        StringBuilder sb = new StringBuilder();
        String noSpaceData = data.replace(" ", "");
        int index = noSpaceData.indexOf(pattern);
        if (index != -1) {
            index += pattern.length();
            sb.append(noSpaceData, index, index + charCount);
        }
        return sb.toString();
    }

    public String extractDataAfterPattern_E(String data, String pattern, int byteCount, boolean addSlashBetweenBytes) {
        StringBuilder sb = new StringBuilder();
        String noSpaceData = data.replace(" ", "");
        int index = noSpaceData.indexOf(pattern);
        if (index != -1) {
            index += pattern.length();
            for (int i = 0; i < byteCount; i++) {
                if (addSlashBetweenBytes && i > 0) sb.append("/");  // 필요 시 '/'로 구분
                int byteIndex = index + i * 2;
                int byteValue = Integer.parseInt(noSpaceData.substring(byteIndex, byteIndex + 2), 16);
                sb.append(byteValue);  // 변환된 값을 10진수로 StringBuilder에 추가
            }
        }
        return sb.toString();
    }

    public String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a) {
            // 바이트 배열을 16진수 문자열로 변환
            sb.append(String.format("%02x", b));
        }
        return sb.toString().toUpperCase();
    }
}