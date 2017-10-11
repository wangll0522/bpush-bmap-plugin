package cn.beyondmap.plugins.push;

import android.util.Log;

import com.beyond.bpush.protobuf.PBAPNSBody;
import com.beyond.bpush.protobuf.PBAPNSMessage;
import com.beyond.bpush.protobuf.PBAPNSUserInfo;
import com.google.gson.Gson;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * Created by mac-pc on 16/8/16.
 */
public class BpushHandler extends ChannelInboundHandlerAdapter {
    private Logger log = LoggerFactory.getLogger(BpushHandler.class);
    private CallbackContext callback;

    public BpushHandler( CallbackContext callback) {
        this.callback = callback;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.i("channelActive: " , ctx.channel().toString());
        BPushClient.channels.addLast(ctx.channel());
    }

    protected void printMsg(ChannelHandlerContext ctx, Object msg){
        PluginResult pluginResult = null;
        byte[] dd = (byte[])msg;
        Log.i("message size: ",  dd.length + "");
        try {
            PBAPNSMessage event = PBAPNSMessage.newBuilder().mergeFrom(dd).build();
            if (event != null && event.getAps() != null && event.getAps().hasSound()) {
                pluginResult = new PluginResult(PluginResult.Status.OK,
                        messageToMap(event));
                pluginResult.setKeepCallback(true);
                callback.sendPluginResult(pluginResult);
                Log.w("channelRead: ", dd.length + " @ " + event);
            } else {
                Log.d("心跳包: ", dd.length +"@"+ "dong!!dong!!!");
            }
        } catch (Exception e) {
            pluginResult = new PluginResult(PluginResult.Status.OK, "{}");
            pluginResult.setKeepCallback(true);
            callback.sendPluginResult(pluginResult);
            e.printStackTrace();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Log.i("channelRead: ", ctx.channel().toString());
        printMsg(ctx, msg);
        ReferenceCountUtil.release(msg);

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
        Log.i("channelInactive: " , ctx.channel().toString());
    }

    public String messageToMap(PBAPNSMessage message) {
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> apsMap = new HashMap<String, Object>();
        Map<String, Object> exts = new HashMap<String, Object>();
        List<PBAPNSUserInfo> userInfos = message.getUserInfoList();
        PBAPNSBody aps = message.getAps();
        apsMap.put("alert", aps.getAlert());
        apsMap.put("badge", aps.getBadge());
        apsMap.put("sound", aps.getSound());
        result.put("aps", apsMap);
        if (userInfos != null) {
            for (int i = 0; i < userInfos.size(); i ++) {
                exts.put(userInfos.get(i).getKey(), userInfos.get(i).getValue());
            }
        }

        result.put("exts", exts);
        result.put("success", true);
        Gson gson = new Gson();
        return gson.toJson(result);
    }
}
