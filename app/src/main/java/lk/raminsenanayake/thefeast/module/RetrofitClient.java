package lk.raminsenanayake.thefeast.module;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;
    public static final String BASE_URL = "http://192.168.8.208:8080/api/";

    public static Retrofit getInstance() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .build();


            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
