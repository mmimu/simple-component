package com.mimu.simple.httpserver;

import com.mimu.simple.httpserver.config.SimpleServerConfigManager;
import com.mimu.simple.httpserver.core.handler.HandlerDispatcher;
import com.mimu.simple.httpserver.core.handler.HttpServerHandler;
import com.mimu.simple.httpserver.core.handler.ServerIdleHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * author: mimu
 * date: 2018/10/21
 */
public class SimpleHttpServer {
    private static final Logger logger = LoggerFactory.getLogger(SimpleHttpServer.class);
    private static Builder builder = new Builder();
    private int contextLength;
    private int port;
    private Class<?> configClazz;

    private SimpleHttpServer(int contextLength, int port, Class<?> config) {
        this.contextLength = contextLength;
        this.port = port;
        this.configClazz = config;
    }

    public void startServer() {
        //ControllerDispatcher controllerDispatcher = new ControllerDispatcher(packages, supportSpring);
        HttpServerHandler handler = new HttpServerHandler(new HandlerDispatcher(configClazz));
        EventLoopGroup connectionGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap server = new ServerBootstrap();
        server.group(connectionGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        ChannelPipeline channelPipeline = channel.pipeline();
                        //channelPipeline.addLast(new LoggingHandler(LogLevel.INFO));
                        channelPipeline.addLast(new HttpRequestDecoder());
                        /*
                          here we use HttpObjectAggregator to compose
                          HttpRequest/HttpResponse/HttpContent/LastHttpContent
                          to FullHttpReqeust or FullHttpResponse
                         */
                        channelPipeline.addLast(new HttpObjectAggregator(contextLength));
                        if (SimpleServerConfigManager.tcp_idle_switch()) {
                            channelPipeline.addLast(new IdleStateHandler(SimpleServerConfigManager.tcp_read_idle_duration(), SimpleServerConfigManager.tcp_write_idle_duration(), 0, TimeUnit.SECONDS));
                            channelPipeline.addLast(new ServerIdleHandler());
                        }
                        channelPipeline.addLast(new HttpResponseEncoder());
                        channelPipeline.addLast(new ChunkedWriteHandler());
                        channelPipeline.addLast(handler);
                    }
                });
        try {
            ChannelFuture channelFuture = server.bind(port).sync();
            logger.info("server start at port {} ...", port);
            channelFuture.channel().closeFuture().addListener((ChannelFutureListener) future -> {
                connectionGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            connectionGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            logger.info("server shutdown ...");
        }));
    }

    public static Builder getServer() {
        return builder;
    }

    public static class Builder {
        private int contextLength = 10 * 1024 * 1024;
        private int port = 8080;
        private Class<?> configClazz;

        public Builder contextLength(int contextLength) {
            this.contextLength = contextLength;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder config(Class<?> config) {
            this.configClazz = config;
            return this;
        }

        public SimpleHttpServer create() {
            return new SimpleHttpServer(contextLength, port, configClazz);
        }
    }
}
