package com.example.opensource_dust;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface location_data {
    @FormUrlEncoded

    @POST("localization/locationcheck/")
    Call<String> sendLocationSensorData(
            @Field("wifidata") String data
    );


}


