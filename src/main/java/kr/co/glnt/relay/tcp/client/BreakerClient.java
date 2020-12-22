package kr.co.glnt.relay.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class BreakerClient {
    private static NioEventLoopGroup loopGroup;
    private static Map<String, Channel> channelMap = new LinkedHashMap<>();

    public void setFeatureCount(int featureCount) {
        if (loopGroup == null) {
            loopGroup = new NioEventLoopGroup(featureCount);
        }
    }

    public void connect(final String host, final int port) {
        Bootstrap bootstrap = new Bootstrap();
        try {
            bootstrap.group(loopGroup).channel(NioSocketChannel.class)
                    .remoteAddress(host, port)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8), new StringEncoder(CharsetUtil.UTF_16));
                            pipeline.addLast(new BreakerMessageHandler());
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect().sync();
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        future.channel().close();
                        bootstrap.connect(host, port).addListener(this);
                    } else {
                        addCloseDetectListener(future.channel());
                    }
                }

                private void addCloseDetectListener(Channel channel) {
                    channel.closeFuture().addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            reconnect(host, port);
                        }
                    });
                }
            });

            channelMap.put(host, channelFuture.channel());
        } catch (Exception e) {
            e.printStackTrace();
            reconnect(host, port);
        }
    }

    private void reconnect(String host, int port) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                connect(host, port);
            }
        }, 1000);
    }

    public void sendMessage(String host, String msg) {
        if (!channelMap.containsKey(host)) {
            return;
        }
        Channel channel = channelMap.get(host);
        ByteBuf byteBuf = Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8);

        channel.writeAndFlush(byteBuf);
    }


}