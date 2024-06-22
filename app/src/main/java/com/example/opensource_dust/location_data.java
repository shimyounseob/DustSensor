package com.example.opensource_dust;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

// // Retrofit Annotation을 사용해 WiFi Scan Data를 서버로 전송하는 API 인터페이스
public interface location_data {
    @FormUrlEncoded
    @POST("localization/locationcheck/")
    Call<String> sendLocationSensorData(
            @Field("wifidata") String data
    );
}


