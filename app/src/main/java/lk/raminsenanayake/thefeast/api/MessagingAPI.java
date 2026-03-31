package lk.raminsenanayake.thefeast.api;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface MessagingAPI {
    @POST("message/welcome")
    @FormUrlEncoded
    Call<Void> createWelcomeMessage(@Field("fcmToken") String fcmToken);

    @POST("message/create-order")
    @FormUrlEncoded
    Call<Void> createOrderCreatedMessage(@Field("fcmToken") String fcmToken);

}
