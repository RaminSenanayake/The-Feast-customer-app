package lk.raminsenanayake.thefeast.api;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


public interface StripeAPI {
    @POST("stripe/create-payment-intent")
    @FormUrlEncoded
    Call<JsonObject> createPaymentIntent(@Field("total") double total);
}
