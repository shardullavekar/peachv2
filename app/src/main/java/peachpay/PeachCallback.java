package peachpay;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

//import devsupport.ai.peachpayv2.R;

public class PeachCallback extends AppCompatActivity {
    private ProgressDialog dialog;

    ApplicationInfo app;

    Bundle bundle;

    String server_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peach_callback);

        dialog = new ProgressDialog(PeachCallback.this);

        app = null;

        try {
            app = getApplicationContext().getPackageManager()
                    .getApplicationInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        bundle = app.metaData;

        server_url = bundle.getString(Config.SERVER_URL);

        String scheme = getIntent().getScheme();

        String storedScheme = bundle.getString(Config.PEACH_CALLBACK_TEMPLATE);

        if (TextUtils.equals(scheme, storedScheme)) {
            String checkoutId = getIntent().getData().getQueryParameter("id");
            CheckStatus checkStatus = new CheckStatus();
            Callback callback = new Callback() {
                @Override
                public void onResponse(String response) {
                    dismissDialogue();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String code = jsonResponse.getJSONObject("result").getString("code");
                        if (TextUtils.equals(code, Config.PEACH_SUCCESS)) {
                            if (jsonResponse.has("registrationId")) {
                                String token = jsonResponse.getString("registrationId");
                                Config.saveTokens(getApplicationContext(), token);
                            }
                        }
                        fireBroadcast(Config.SUCCESS, response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        fireBroadcast(Config.FAILED, response);
                    }

                }
            };
            showDialogue("Fetching Status");
            checkStatus.postStatus(server_url, checkoutId, "PA", callback);
        }
    }

    private void showDialogue(String message) {
        dialog.setMessage(message);
        dialog.show();
    }

    private void dismissDialogue() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void fireBroadcast(int code, String message) {
        Intent intent = new Intent();
        intent.setAction("ai.devsupport.peachpay");
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.putExtra("code", code);
        intent.putExtra("response", message);
        sendBroadcast(intent);
        PeachCallback.this.finish();
    }
}
