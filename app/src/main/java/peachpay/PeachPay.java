package peachpay;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.oppwa.mobile.connect.BuildConfig;
import com.oppwa.mobile.connect.checkout.dialog.CheckoutActivity;
import com.oppwa.mobile.connect.checkout.meta.CheckoutSettings;
import com.oppwa.mobile.connect.checkout.meta.CheckoutStorePaymentDetailsMode;
import com.oppwa.mobile.connect.exception.PaymentError;
import com.oppwa.mobile.connect.exception.PaymentException;
import com.oppwa.mobile.connect.provider.Connect;
import com.oppwa.mobile.connect.provider.Transaction;
import com.oppwa.mobile.connect.provider.TransactionType;
import com.oppwa.mobile.connect.service.ConnectService;
import com.oppwa.mobile.connect.service.IProviderBinder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.Set;

import devsupport.ai.peachpayv2.R;

//import devsupport.ai.peachpayv2.R;


public class PeachPay extends AppCompatActivity {
    private IProviderBinder binder;

    private ServiceConnection serviceConnection;

    String server_url,
            amount, currency, type, env, checkoutId, savecard, recurring = "";

    ApplicationInfo app;

    Bundle bundle;

    private ProgressDialog dialog;

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, ConnectService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
        stopService(new Intent(this, ConnectService.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = null;

        dialog = new ProgressDialog(PeachPay.this);

        try {
            app = getApplicationContext().getPackageManager()
                    .getApplicationInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        bundle = app.metaData;

        amount = getIntent().getStringExtra("amount");

        currency = getIntent().getStringExtra("currency");

        type = getIntent().getStringExtra("type");

        savecard = getIntent().getStringExtra("savecard");

        env = getIntent().getStringExtra("env");

        if (getIntent().hasExtra("recurring")) {
            recurring = getIntent().getStringExtra("recurring");
        }

        server_url = bundle.getString(Config.SERVER_URL);

        if (checkValidation()) {
            initEnv();
            setContentView(R.layout.peach_pay);
            getCheckoutId();
        }
    }

    private void initEnv() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = (IProviderBinder) service;

                try {
                    if (TextUtils.equals(env, Config.TEST)) {
                        binder.initializeProvider(Connect.ProviderMode.TEST);
                    }
                    else if (TextUtils.equals(env, Config.TEST)){
                        binder.initializeProvider(Connect.ProviderMode.LIVE);
                    }
                    else {
                        fireBroadcast(Config.FAILED, "Invalid Environment");
                    }
                } catch (PaymentException ee) {
                    fireBroadcast(Config.FAILED, ee.getMessage());
                }

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                binder = null;
            }
        };
    }

    private void getCheckoutId() {
        Checkout checkout = new Checkout();
        Callback callback = new Callback() {
            @Override
            public void onResponse(String response) {
                dismissDialogue();
                JSONObject jsonObject;
                try {
                    jsonObject = new JSONObject(response);
                    JSONObject result = jsonObject.getJSONObject("result");
                    if (TextUtils.equals(result.getString("code"), Config.PEACH_SUCCESS) || TextUtils.equals(result.getString("code"), Config.PEACH_SUCCESS_2)) {
                        configCheckout(jsonObject.getString("id"));
                    }
                    else {
                        fireBroadcast(Config.FAILED, result.getString("description"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    fireBroadcast(Config.FAILED, "Unable to fetch failure reason");
                }
            }
        };
        String tokens = Config.getTokens(getApplicationContext());

        checkout.post(server_url, amount, currency, type, recurring, tokens, callback);
        showDialogue("Getting Checkout Id");
    }

    private void getStatus(String resourcepath) {
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
        checkStatus.post(server_url, resourcepath, type, callback);
    }

    private void configCheckout(String checkoutId) {
        this.checkoutId = checkoutId;
        Set<String> paymentBrands = new LinkedHashSet<>();

        paymentBrands.add("VISA");
        paymentBrands.add("MASTER");
        paymentBrands.add("DIRECTDEBIT_SEPA");

        CheckoutSettings checkoutSettings = new CheckoutSettings(checkoutId, paymentBrands);
        if (TextUtils.equals(savecard, Config.ALWAYS)) {
            checkoutSettings.setStorePaymentDetailsMode(CheckoutStorePaymentDetailsMode.ALWAYS);
        }
        else if (TextUtils.equals(savecard, Config.PROMPT)) {
            checkoutSettings.setStorePaymentDetailsMode(CheckoutStorePaymentDetailsMode.PROMPT);
        }
        else {
            checkoutSettings.setStorePaymentDetailsMode(CheckoutStorePaymentDetailsMode.NEVER);
        }
        checkoutSettings.setWebViewEnabledFor3DSecure(true);

        ComponentName componentName = new ComponentName(BuildConfig.APPLICATION_ID,
                CheckoutBroadcastReceiver.class.getCanonicalName());

        Intent intent = new Intent(PeachPay.this, CheckoutActivity.class);
        intent.putExtra(CheckoutActivity.CHECKOUT_SETTINGS, checkoutSettings);

        startActivityForResult(intent, CheckoutActivity.CHECKOUT_ACTIVITY);
    }

    private boolean checkValidation() {
        if (TextUtils.isEmpty(server_url)) {
            fireBroadcast(Config.FAILED, "Invalid Order URL");
            return false;
        }

        if (TextUtils.isEmpty(amount)) {
            fireBroadcast(Config.FAILED, "Invalid Amount");
            return false;
        }

        if (TextUtils.isEmpty(currency)) {
            fireBroadcast(Config.FAILED, "Invalid Currency");
            return false;
        }

        if (TextUtils.isEmpty(type)) {
            fireBroadcast(Config.FAILED, "Invalid Type");
            return false;
        }

        if (TextUtils.isEmpty(env)) {
            fireBroadcast(Config.FAILED, "Invalid Environment");
            return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case CheckoutActivity.RESULT_OK:
            /* transaction completed */
                Transaction transaction = data.getParcelableExtra(CheckoutActivity.CHECKOUT_RESULT_TRANSACTION);

            /* resource path if needed */
                String resourcePath = data.getStringExtra(CheckoutActivity.CHECKOUT_RESULT_RESOURCE_PATH);

                if (transaction.getTransactionType() == TransactionType.SYNC) {
                    getStatus(resourcePath);
                } else {
                    endActivity();
                }

                break;
            case CheckoutActivity.RESULT_CANCELED:
                fireBroadcast(Config.FAILED, "Shoper cancelled transaction");
                break;
            case CheckoutActivity.RESULT_ERROR:
                PaymentError error = data.getParcelableExtra(CheckoutActivity.CHECKOUT_RESULT_ERROR);
                fireBroadcast(Config.FAILED, error.getErrorMessage());
                break;
            default:
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getScheme().equals("devsupport")) {
            String checkoutId = intent.getData().getQueryParameter("id");
            fireBroadcast(Config.SUCCESS, "checkoutId=" + checkoutId);
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
        PeachPay.this.finish();
    }

    private void endActivity() {
        PeachPay.this.finish();
    }
}
