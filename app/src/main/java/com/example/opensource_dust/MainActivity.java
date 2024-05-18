package com.example.opensource_dust;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static final String[] MAC_ADDRS = {

//            // 1조
            "D8:3A:DD:42:AC:7F",
            "D8:3A:DD:42:AC:64",
            "B8:27:EB:DA:F2:5B",
            "B8:27:EB:0C:F3:83",

            // 2조
            "D8:3A:DD:79:8F:97",
            "D8:3A:DD:79:8F:B9",
            "D8:3A:DD:79:8F:54",
            "D8:3A:DD:79:8F:80",

            // 3조
            "D8:3A:DD:79:8E:D9",
            "D8:3A:DD:42:AC:9A",
            "D8:3A:DD:42:AB:FB",
            "D8:3A:DD:79:8E:9B",

//            // 4조
            "D8:3A:DD:78:A7:1A",
            "D8:3A:DD:79:8E:BF",
            "D8:3A:DD:79:8E:92",
            "D8:3A:DD:79:8F:59",
//            // 5조
            "B8:27:EB:47:8D:50",
            "B8:27:EB:D3:40:06",
            "B8:27:EB:E4:D0:FC",
            "B8:27:EB:57:71:7D"
    };

    public static final String[] MAC_ADDRS1 = {
            "D8:3A:DD:42:AC:7F",
            "D8:3A:DD:42:AC:64",
            "B8:27:EB:DA:F2:5B",
            "B8:27:EB:0C:F3:83"};
    public static final String[] MAC_ADDRS2 = {
            "D8:3A:DD:79:8F:97",
            "D8:3A:DD:79:8F:B9",
            "D8:3A:DD:79:8F:54",
            "D8:3A:DD:79:8F:80"};
    public static final String[] MAC_ADDRS4 = {
            "D8:3A:DD:78:A7:1A",
            "D8:3A:DD:79:8E:BF",
            "D8:3A:DD:79:8E:92",
            "D8:3A:DD:79:8F:59"
    };
    public static final String[] MAC_ADDRS5 = {
            "B8:27:EB:47:8D:50",
            "B8:27:EB:D3:40:06",
            "B8:27:EB:E4:D0:FC",
            "B8:27:EB:57:71:7D"};
    public static final String[] MAC_ADDRS3 = {
            "D8:3A:DD:79:8E:D9",
            "D8:3A:DD:42:AC:9A",
            "D8:3A:DD:42:AB:FB",
            "D8:3A:DD:79:8E:9B"
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

    private BluetoothAdapter blead;

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
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    // BLE Scan 시작
                    // scancallback_le 함수를 통해 원하는 동작 실행
                    blead.startLeScan(scancallback_le);
                    txtLog.append("\n스캔 시작");
                }
            }
        });

        btnStop = (Button) findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                txtPassword.setText("Stop");
//                txtLog.append("\nStop");
                if (checkPermission()) {
                    // BLE Scan 중지
                    blead.stopLeScan(scancallback_le);
                    txtLog.append("\n스캔 중지");
                }
            }
        });

        Gson gson = new GsonBuilder().setLenient().create();
        retrofit2 = new Retrofit.Builder()
                // 기본 URL을 설정
                .baseUrl(BASE_URL)
                // Response를 String 형태로 받을 때
                .addConverterFactory(ScalarsConverterFactory.create())
                // Response를 Json 형태로 받을 때
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        txtResult = (TextView) findViewById(R.id.txt_result);
        txtPassword = (TextView) findViewById(R.id.txt_password);
        txtTime = (TextView) findViewById(R.id.txt_time);
        txtData = (TextView) findViewById(R.id.txt_data);
        txtLog = (TextView) findViewById(R.id.txt_log);
        txtLog.setMovementMethod(new ScrollingMovementMethod());
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
        }
    }

    private BluetoothAdapter.LeScanCallback scancallback_le = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            String macAdd = device.getAddress();
            String data = byteArrayToHex(scanRecord);
            Log.i("스캔 Mac", macAdd);
            Log.i("스캔 Data", data);
            for (String validMac : MAC_ADDRS) {
                if (macAdd.equalsIgnoreCase(validMac)) {
                    detectedMacAddress = device.getAddress();  // 검색된 MAC 주소를 멤버 변수에 저장
                    txtLog.append("\n" + macAdd);
                    txtLog.append("\n" + data);

                    // OTP 추출 ("f0f0" 뒤에 오는 데이터를 연속된 숫자로 처리)
                    String Otp = extractDataAfterPattern(data, "F0F0", 3, false);

                    // Sensing Time 추출 ("9999" 뒤에 오는 데이터를 연속된 숫자로 처리)
                    String sensingTime = extractDataAfterPattern(data, "9999", 5, false);

                    // Sensor Data 추출 및 파싱 (각 센서 데이터 값 사이에 "/"를 넣음)
                    String sensorData = extractDataAfterPattern(data, "FD", 3, true);

                    // 추출된 데이터를 TextView에 설정
                    txtPassword.setText(Otp);
                    txtTime.setText(sensingTime);
                    txtData.setText(sensorData);

                    // 로그에 추출된 데이터를 출력
                    txtLog.append("\nTime OTP: " + Otp);
                    txtLog.append("\nSensing Time: " + sensingTime);
                    txtLog.append("\nSensor Data: " + sensorData);

                    sendSensorData(macAdd, data);
                }
            }
        }

        private void sendSensorData(String detectedMacAddress, String data) {
            String sensorValue = null;

            for (String validMac : MAC_ADDRS1) {
                if (detectedMacAddress.equalsIgnoreCase(validMac)) {
                    sensorValue = "1jo";
                    break; // 일치하는 MAC 주소를 찾으면 루프 종료
                }
            }
            if (sensorValue == null) {
                for (String validMac : MAC_ADDRS2) {
                    if (detectedMacAddress.equalsIgnoreCase(validMac)) {
                        sensorValue = "2jo";
                        break;
                    }
                }
            }
            if (sensorValue == null) {
                for (String validMac : MAC_ADDRS3) {
                    if (detectedMacAddress.equalsIgnoreCase(validMac)) {
                        sensorValue = "3jo";
                        break;
                    }
                }
            }
            if (sensorValue == null) {
                for (String validMac : MAC_ADDRS4) {
                    if (detectedMacAddress.equalsIgnoreCase(validMac)) {
                        sensorValue = "4jo";
                        break;
                    }
                }
            }
            if (sensorValue == null) {
                for (String validMac : MAC_ADDRS5) {
                    if (detectedMacAddress.equalsIgnoreCase(validMac)) {
                        sensorValue = "5jo";
                        break;
                    }
                }
            }
            // 센서 값이 설정되면, 추가 데이터를 추출하고 서버로 전송
            String receiverValue = "3jo"; // 수신자 값 설정
            String otpValue = txtPassword.getText().toString(); // OTP 값 추출
            String timeValue = txtTime.getText().toString(); // 시간 값 추출
            String dataValue = txtData.getText().toString(); // 데이터 값 추출

            // comm_data 인터페이스의 인스턴스 생성
            comm_data service = retrofit2.create(comm_data.class);

            // 서버로 전송할 데이터 구성 및 전송
            Call<String> call = service.sendSensorData(sensorValue, detectedMacAddress, receiverValue, timeValue, otpValue, dataValue);
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        txtResult.setText("Success: " + response.body());
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




        public String byteArrayToHex(byte[] a) {
            StringBuilder sb = new StringBuilder(a.length * 2);
            for (byte b : a) {
                // 바이트 배열을 16진수 문자열로 변환
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase();
        }
    };
}