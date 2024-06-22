package com.example.opensource_dust;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface air_comm_data {
    @FormUrlEncoded

    @POST("airquality/sensing/")
    Call<String> sendAirSensorData(
            @Field("sensor") String sensor,
            @Field("mode") String mode,
            @Field("mac") String mac,
            @Field("receiver") String receiver,
            @Field("time") String time,
            @Field("otp") String otp,
            @Field("key") String key,
            @Field("data") String data
    );


}


