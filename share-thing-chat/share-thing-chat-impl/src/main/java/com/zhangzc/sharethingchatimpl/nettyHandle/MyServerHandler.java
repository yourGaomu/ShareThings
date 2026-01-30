package com.zhangzc.sharethingchatimpl.nettyHandle;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class MyServerHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // 处理接收到的消息，这里只是简单地打印出来
        System.out.println("Server received: " + msg);
        // 你可以在这里添加更复杂的业务逻辑，比如解析消息、访问数据库等。
    }
}
