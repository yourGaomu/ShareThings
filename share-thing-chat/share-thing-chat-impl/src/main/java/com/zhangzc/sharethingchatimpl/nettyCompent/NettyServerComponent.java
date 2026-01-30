package com.zhangzc.sharethingchatimpl.nettyCompent;



import com.zhangzc.sharethingchatimpl.nettyService.NettyServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NettyServerComponent {

    private NettyServer nettyServer;

    @PostConstruct
    public void startServer() {
        nettyServer = new NettyServer(8080); // 配置端口号
        try {
            nettyServer.start(); // 启动Netty服务
        } catch (Exception e) {
            log.error("Netty服务启动失败", e);
            e.printStackTrace(); // 实际项目中应该使用日志记录异常信息
            // 处理启动失败的情况，比如停止Spring Boot应用
            System.exit(1);
        }
    }

    @PreDestroy
    public void stopServer() {
        if (nettyServer != null) {
            // 这里需要实现NettyServer的优雅关闭逻辑
            // 通常需要关闭EventLoopGroup并等待当前处理的任务完成
            nettyServer.stop(); // 假设NettyServer有一个stop方法来关闭服务
        }
    }
}
