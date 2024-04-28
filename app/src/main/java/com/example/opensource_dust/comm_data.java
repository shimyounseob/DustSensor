package com.example.opensource_dust;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface comm_data {
    @FormUrlEncoded
    @POST("dustsensor/sensing/")
    Call<String> sendSensorData(
            @Field("sensor") String sensor,
            @Field("mac") String mac,
            @Field("receiver") String receiver,
            @Field("time") String time,
            @Field("otp") String otp,
            @Field("data") String data
    );
}

