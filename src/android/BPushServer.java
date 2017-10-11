package cn.beyondmap.plugins.push;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by mac-pc on 16/8/16.
 */
public class BPushServer extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
