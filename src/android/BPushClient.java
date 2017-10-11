package cn.beyondmap.plugins.push;

import android.util.Log;

import com.beyond.bpush.protobuf.PBAPNSEvent;

import org.apache.cordova.CallbackContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by mac-pc on 16/8/16.
 */
public class BPushClient {
    private static final String appkey = "da6ae142e0e009149e4a365d";
    private static final String token = "LL_WANG_TOKEN";
    private static BPushClient instance = null;
    private final Bootstrap b = new Bootstrap(); // (1)
    private EventLoopGroup workerGroup;
    public static LinkedBlockingDeque<Channel> channels = new LinkedBlockingDeque<Channel>();
    private BPushClient() {}
    private Logger log = LoggerFactory.getLogger(BpushHandler.class);

    private Thread thread, pingThread;

    public static synchronized BPushClient getInstance() {
        if (instance == null) {
            instance = new BPushClient();
        }
        return instance;
    }

    public void start(final Integer port, final String host, final String userid, final String deviceId, final CallbackContext callback){
        Log.w("BpushClient:start:", port+","+ host+","+userid+","+ deviceId +"");
        //连接到push服务器
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                connect(port, host, callback);
            }
        });

        thread.start();

        //发心跳包 ／ 每隔10s
        pingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (true) {
                    try {
                        ping(userid, deviceId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(10 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        pingThread.start();
    }

    private void ping(String userId, String devicerId) throws IOException {
        int total = channels.size();
        if(total == 0){
            return;
        }
        Channel channel = channels.pop();
        PBAPNSEvent event = PBAPNSEvent.newBuilder()
                .setOp(PBAPNSEvent.Ops.Online_VALUE)
                .setAppKey(BPushClient.appkey)
                .setUserId(userId)
                .setDeviceId(devicerId)
                .setToken(BPushClient.token)
                .setTypeId(PBAPNSEvent.DeviceTypes.Android_VALUE).build();
        send(channel, event);
        channels.addLast(channel);
    }

    public void send(String userId, String devicerId) throws IOException {
        Channel channel = channels.pop();
        PBAPNSEvent event = PBAPNSEvent.newBuilder()
                .setOp(PBAPNSEvent.Ops.Online_VALUE)
                .setAppKey(BPushClient.appkey)
                .setUserId(userId)
                .setDeviceId(devicerId)
                .setToken(BPushClient.token)
                .setTypeId(PBAPNSEvent.DeviceTypes.Android_VALUE).build();
        send(channel, event);
        channels.addLast(channel);
    }

    private void send(Channel c, PBAPNSEvent event) throws IOException {
        byte[] bytes = event.toByteArray();
        final ByteBuf data = c.config().getAllocator().buffer(bytes.length); // (2)
        data.writeBytes(bytes);
        ChannelFuture cf = c.writeAndFlush(data);
        cf.addListener(new GenericFutureListener<Future<? super Void>>() {

            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                System.out.println(future);
            }
        });

    }

    public void close() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if (pingThread != null) {
            pingThread.interrupt();
            pingThread = null;
        }
    }

    private void connect(Integer sport, String shost,  final CallbackContext callback){
        final Integer port = sport == null ? 9080 : sport;
        final String host = shost == null ? "localhost" : shost;
        final int pool = 2;
        Log.w("BpushClient:connect:", port+","+ host);
        workerGroup = new NioEventLoopGroup(pool);
        try {
            b.group(workerGroup); // (2)
            b.channel(NioSocketChannel.class); // (3)
            b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
            b.option(ChannelOption.TCP_NODELAY, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {

                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                    pipeline.addLast("bytesDecoder",new ByteArrayDecoder());

                    pipeline.addLast("frameEncoder", new LengthFieldPrepender(4, false));
                    pipeline.addLast("bytesEncoder", new ByteArrayEncoder());

                    pipeline.addLast("handler", new BpushHandler(callback));
                }
            });

            final List<ChannelFuture> fs = new ArrayList<ChannelFuture>();
            // Start the client.
            for(int i=0; i<pool; i++){
                ChannelFuture f = b.connect(host, port); // (5)
                if(f.cause() != null){
                    f.cause().printStackTrace();
                    continue;
                }
                fs.add(f);
            }

            for (ChannelFuture f : fs){
                if (!f.isDone()) {
                    f.get();
                }
            }

            Log.w("BPUshClient", "BPush server. connected.");

        } catch (Exception e){
            e.printStackTrace();
            workerGroup.shutdownGracefully();
        }
    }



}
