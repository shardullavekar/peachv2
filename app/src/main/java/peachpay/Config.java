package peachpay;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by shardullavekar on 16/09/17.
 */

public class Config {
    public static final String SERVER_URL = "server.url.peachpay";
    public final static int SUCCESS = 10;
    public final static int FAILED = 20;
    public final static int PEACHPAY = 30;
    public final static String TEST = "test";
    public final static String PROD = "prod";
    public final static String PEACH_SUCCESS = "000.100.110";
    public final static String PEACH_SUCCESS_2 = "000.200.100";
    public final static String ALWAYS = "always";
    public final static String NEVER = "never";
    public final static String PROMPT = "prompt";
    public final static String TOKENS = "tokens";
    public final static String PEACH_CALLBACK_TEMPLATE = "peachpay.callback.name";

    public static void saveTokens(Context context, String token) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(TOKENS, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(token, "1");
        editor.commit();
        editor.apply();
    }

    public static String getTokens(Context context) {
        JSONArray jsonArray = new JSONArray();
        SharedPreferences sharedPreferences = context.getSharedPreferences(TOKENS, 0);
        Map<String, ?> allEntries = sharedPreferences.getAll();

        if (allEntries.isEmpty()) {
            return "";
        }

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            try {
                JSONObject tokenJson = new JSONObject();
                tokenJson.put("id", entry.getKey());
                jsonArray.put(tokenJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return jsonArray.toString();
    }
}
