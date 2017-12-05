package peachpay;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by shardullavekar on 19/08/17.
 */

public class Post {
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    public String getId(String url, String action, String recurring, String tokens, String amount, String currency, String type) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("amount", amount)
                .add("currency", currency)
                .add("paymentType", type)
                .add("action", action)
                .add("recurring", recurring)
                .add("registrations", tokens)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public String getStatus(String url, String action, String resourcePath, String type) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("resourcePath", resourcePath)
                .add("action", action)
                .add("paymentType", type)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

}
