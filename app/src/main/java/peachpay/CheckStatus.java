package peachpay;

import android.os.AsyncTask;

import java.io.IOException;

/**
 * Created by shardullavekar on 01/12/17.
 */

public class CheckStatus {
    public CheckStatus () {

    }
    public void post(String url, String resourcePath, String type, Callback callback) {
        CheckStatusAsynch checkoutAsynch = new CheckStatusAsynch(callback);
        checkoutAsynch.execute(new String[]{url, "getPaymentStatus", resourcePath, type});
    }

    public void postStatus(String url, String checkoutId, String type, Callback callback) {
        CheckStatusAsynch checkStatusAsynch = new CheckStatusAsynch(callback);
        checkStatusAsynch.execute(new String[]{url, "getPaymentStatus", "/v1/checkouts/" + checkoutId + "/payment", type});

    }

    private class CheckStatusAsynch extends AsyncTask<String, Void, String> {
        Callback callback;
        public CheckStatusAsynch(Callback callback) {
            this.callback = callback;
        }
        @Override
        protected String doInBackground(String... params) {
            Post post = new Post();
            String checkoutResponse;
            try {
                checkoutResponse = post.getStatus(params[0], params[1], params[2], params[3]);
            } catch (IOException e) {
                e.printStackTrace();
                return "ioException - check your net connection";
            }
            return checkoutResponse;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            this.callback.onResponse(s);
        }
    }
}

