package cn.beyondmap.plugins.push;

import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by mac-pc on 16/8/16.
 */
public class BPushPlugin extends CordovaPlugin {

    BPushClient pushClient = BPushClient.getInstance();
    private String userId;
    private String deviceId;
    private String host = "beyondmap.cn";
    private Integer port = 8090;

    private void start(final CallbackContext callbackContext) {
        pushClient.start(port, host, userId, deviceId, callbackContext);
    }

    private void stop() {
        pushClient.close();
    }



    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        final CallbackContext callback = callbackContext;
        if (action.equals("bind")) {
            userId = args.getString(0);
            deviceId = args.getString(1);
            port = args.getInt(2);
            host = args.getString(3);

            if (userId != null && !"".equals(userId)) {
                stop();
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            start(callback);
                        }catch (Exception e) {
                            Log.w("GpsUploadPlugin", e.getMessage());
                            stop();
                        }
                    }
                });
            } else {
                callbackContext.error("{flag:-1,error:'用户别名不能为空'}");
            }
            return true;
        } else if ("unbind".equals(action)) {
            android.widget.Toast.makeText(cordova.getActivity(), "解绑成功", android.widget.Toast.LENGTH_LONG).show();
            this.stop();
            callbackContext.success("{flag:1,msg:'解绑成功'}");
        }
        return super.execute(action, args, callbackContext);
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
    }

    @Override
    public Object onMessage(String id, Object data) {
        return super.onMessage(id, data);
    }


}
