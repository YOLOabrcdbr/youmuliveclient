package com.teachk.publisher;

import org.json.JSONObject;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import  okhttp3.OkHttpClient;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;



public class GetLiveInf {
    public interface Live{
        @GET("com/{cid}/liveroom")
        Call<ResponseBody>GoRoom(@Path("cid") String cid,@Query("aid") String aid,@Query("lid") String lid);

        @GET("com/{cid}/liverooms")
        Call<ResponseBody>GetRoom(@Path("cid") String cid,@Query("aid") String aid);


        @Headers({"Content-Type: application/json","Accept: application/json"})
        @POST("user/login/")
        Call<ResponseBody>Login(@Body RequestBody requestBody);
    }
}
