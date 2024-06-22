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

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // 스캔할 모든 라즈베리 파이의 MAC Address
    public static final String[] MAC_ADDRS = {

            "D8:3A:DD:C1:88:E8",
            "D8:3A:DD:C1:89:5B",
            "D8:3A:DD:C1:88:BD",
            "D8:3A:DD:C1:88:D7",
            "D8:3A:DD:79:8E:D9",
            "D8:3A:DD:42:AC:9A",
            "D8:3A:DD:42:AB:FB",
            "D8:3A:DD:79:8E:9B"
    };

    // Dust Sensor 라즈베리 파이의 MAC Address
    public static final String[] DUST_ADDRS = {
            "D8:3A:DD:79:8E:D9",
            "D8:3A:DD:42:AC:9A",
            "D8:3A:DD:42:AB:FB",
            "D8:3A:DD:79:8E:9B"
    };

    // Air Quality Sensor 라즈베리 파이의 MAC Address
    public static final String[] AIR_ADDRS = {
            "D8:3A:DD:C1:88:E8",
            "D8:3A:DD:C1:89:5B",
            "D8:3A:DD:C1:88:BD",
            "D8:3A:DD:C1:88:D7"
    };

    // 기본 주소 URL
    public static final String BASE_URL = "http://203.255.81.72:10021/";
    public Button btnScan;
    public Button btnStop;
    public TextView txtPassword;
    public TextView txtTime;
    public TextView txtData;
    public TextView txtResult;
    public TextView txtLog;
    private String detectedMacAddress;
    private Retrofit retrofit2;
    private WifiManager wifiman;
    private BluetoothAdapter blead;
    String wifidata;
    String id;
    String key;

    // 사용자가 스캔 버튼 누르면 WiFi 스캔 시작
    void wifiStart() {
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

        // Android studio 권한 자동 확인(내 폰에서 BLUETOOTH_SCAN Permission 추가)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1000);
        }

        // Bluetooth Adapter 객체 생성
        blead = BluetoothAdapter.getDefaultAdapter();

        // Bluetooth adapter를 사용하지 못하는 상태면 사용 가능하게 설정
        if (!blead.isEnabled())
            blead.enable();

        btnScan = (Button) findViewById(R.id.btn_scan);

        // Wifi Manager 객체 생성
        wifiman = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiman == null) {
            txtLog.append("\nWifiManager 초기화 실패");
            return;
        }

        // 특정 Broadcast Message만을 수신하기 위한 Intent Filter 생성
        // Wifi Scan 결과를 수신하기 위한 RssiReceiver 등록
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(rssiReceiver, filter);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    // 디바이스 ID를 가져옴
                    id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

                    blead.startLeScan(scancallback_le);
                    txtLog.append("\n스캔 시작");
                    try {
                        wifiStart();
                    } catch (Exception e) {
                        txtLog.append("\n" + e);
                    }
                    txtLog.append("\n" + id);
                }
            }
        });

        // 중지 버튼을 누르면 블루투스 스캔이 정지
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    blead.stopLeScan(scancallback_le);
                    txtLog.append("\n스캔 중지");
                }
            }
        });

        // Json 형식 데이터 처리 위한 Gson 객체 생성
        Gson gson = new GsonBuilder().setLenient().create();
        retrofit2 = new Retrofit.Builder() // 서버와 통신 위해 retrofit 객체 초기화
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
                // Wi-Fi 스캔 결과가 사용 가능한 경우 scanSuccess 실행
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                    boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                    if (success) {
                        scanSuccess();
                    } else {
                        txtLog.append("\nBRoadwifi스캔 실패");
                    }
                }
            } catch (Exception e) {
                txtLog.append("\nWiFi 스캔 결과 처리 중 예외 발생: " + e.getMessage());
            }
        }
    };

    private void scanSuccess() {
        if (!checkPermission()) {
            return;
        }
        List<ScanResult> scanresult = wifiman.getScanResults();
        for (int i = 0; i < scanresult.size(); i++) {
            int RSSI = scanresult.get(i).level; // 수신 신호 강도
            String BSSID = scanresult.get(i).BSSID; // 라즈베리 파이 MAC 주소
            // Wi-Fi 스캔 결과를 wifidata 문자열에 추가
            wifidata += (BSSID + "!" + String.valueOf(RSSI) + "/");
        }

        // Retrofit 인터페이스를 사용하여 Wi-Fi 데이터 전송
        location_data location_service = retrofit2.create(location_data.class);
        Call<String> call = location_service.sendLocationSensorData(wifidata);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    // 서버로부터 성공적인 응답을 받으면 로그에 메시지 추가
                    txtLog.append("\n*********************위치Success*******************: \n\n" + response.body());
                    key = response.body(); // 응답으로 받은 키 값을 저장
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

    // 안드로이드 권한 요청 메소드
    private boolean checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            return false;
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1000);
            return false;
        } else {
            return true;
        }
    }

    private BluetoothAdapter.LeScanCallback scancallback_le = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            try {
                String macAdd = device.getAddress();
                String data = byteArrayToHex(scanRecord); // 스캔 데이터를 16진수 문자열로 변환

                // 유효한 MAC 주소인지 확인
                for (String validMac : MAC_ADDRS) {
                    if (macAdd.equalsIgnoreCase(validMac)) {
                        detectedMacAddress = device.getAddress();  // 검색된 MAC 주소를 멤버 변수에 저장
                        txtLog.append("\n\n\n******* 데이터 확인 *******\n" + macAdd);
                        txtLog.append("\n" + data);
                        Integer IsDust = -1;
                        String sensorData = "";

                        for (String DustMac : DUST_ADDRS) {
                            if (detectedMacAddress.equalsIgnoreCase(DustMac)) {
                                sensorData = extractDataAfterPattern(data, "FD", 3, true);
                                txtData.setText(sensorData);
                                txtLog.append("\nSensor Data: " + sensorData);
                                txtLog.append("\nDUST_DATA");
                                IsDust = 1;//Dust Sensor이면 1
                                break;
                            }
                        }
                        for (String AirMac : AIR_ADDRS) {
                            if (detectedMacAddress.equalsIgnoreCase(AirMac)) {
                                sensorData = extractDataAfterPattern(data, "FD", 2, false);
                                txtData.setText(sensorData);
                                txtLog.append("\nSensor Data: " + sensorData);
                                txtLog.append("\nAIR_DATA");
                                IsDust = 0;//Air Sensor이면 0
                                break;
                            }
                        }

                        // OTP와 Sensing Time 파싱
                        String Otp = extractDataAfterPattern(data, "F0F0", 3, false);
                        String sensingTime = extractDataAfterPattern(data, "9999", 5, false);
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
            }
        }
    };

    // 센서 데이터를 서버로 전송
    private void sendSensorData(String detectedMacAddress, String data, Integer IsDust) {
        try {
            String otpValue = txtPassword.getText().toString(); // OTP 값 추출
            String timeValue = txtTime.getText().toString(); // 시간 값 추출
            String dataValue = txtData.getText().toString(); // 데이터 값 추출

            // Dust Sensor인 경우
            if (IsDust == 1 && key != null) {
                // Retrofit 인터페이스를 사용하여 Dust Sensor 데이터 전송
                dust_comm_data dust_service = retrofit2.create(dust_comm_data.class);
                Call<String> call = dust_service.sendDustSensorData("3jo", "advertising", detectedMacAddress, id, timeValue, otpValue, key, dataValue);
                txtLog.append("\n위치 정보 : " + key);
                txtLog.append("\n3jo" + "advertising" + detectedMacAddress + id + timeValue + otpValue + key + dataValue);

                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        Log.e("받은 데이터", response.body().toString());
                        if (response.isSuccessful()) {
                            txtLog.append("더스트 응답 받기 성공" + response.toString());
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

            // Air Quality Sensor인 경우
            if (IsDust == 0 && key != null) {
                // Retrofit 인터페이스를 사용하여 Air Quality Sensor 데이터 전송
                air_comm_data air_service = retrofit2.create(air_comm_data.class);
                Call<String> call = air_service.sendAirSensorData("3jo", "advertising", detectedMacAddress, id, timeValue, otpValue, key, dataValue);
                txtLog.append("\n위치 정보 : " + key);
                txtLog.append("\n3jo" + "advertising" + detectedMacAddress + id + timeValue + otpValue + key + dataValue);

                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful()) {
                            txtLog.append("에어 응답 받기 성공 " + response.toString());
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
        }
    }

    /*
       주어진 패턴 이후의 데이터를 파싱함
       pattern 이후의 bytecount 만큼 파싱하고
       addSlashBetweenBytes가 True 일 경우, '/'를 바이트 사이에 추가함
     */
    public String extractDataAfterPattern(String data, String pattern, int byteCount, boolean addSlashBetweenBytes) {
        StringBuilder sb = new StringBuilder();
        String noSpaceData = data.replace(" ", ""); //공백 제거
        int index = noSpaceData.indexOf(pattern); //패턴의 시작 인덱스 찾기

        if (index != -1) {
            index += pattern.length(); // 패턴 이후의 데이터를 추출하기 위해 인덱스를 패턴 길이만큼 이동
            for (int i = 0; i < byteCount; i++) {
                if (addSlashBetweenBytes && i > 0) sb.append("/");  // 필요 시 '/'로 구분
                int byteIndex = index + i * 2;
                int byteValue = Integer.parseInt(noSpaceData.substring(byteIndex, byteIndex + 2), 16);
                if (byteValue < 10) // 10 미만의 값 앞에 0 추가
                    sb.append(0);
                if (byteValue == 0)
                    sb.append(0);
                sb.append(byteValue);  // 변환된 값을 10진수로 StringBuilder에 추가
            }
        }
        return sb.toString();
    }

    // 바이트 배열을 16진수 문자열로 변환
    public String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        // 바이트 배열 a의  각 바이트 b를 확인
        for (byte b : a) {
            // String.format을 사용해 바이트 b를 2자리 16진수 문자열로 변환
            sb.append(String.format("%02x", b));
        }
        return sb.toString().toUpperCase();
    }
}