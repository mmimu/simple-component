package com.mimu.simple.httpserver.core.handler;

import com.mimu.simple.httpserver.core.request.SimpleHttpRequest;
import com.mimu.simple.httpserver.core.response.SimpleHttpResponse;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;


/**
 * author: mimu
 * date: 2018/10/22
 */
@ChannelHandler.Sharable
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);
    private static final Logger serverLogger = LoggerFactory.getLogger("serverLogger");

    private HandlerDispatcher dispatcher;

    public HttpServerHandler(HandlerDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) {
        long startTime = System.currentTimeMillis();
        SimpleHttpRequest simpleHttpRequest = new SimpleHttpRequest(channelHandlerContext.channel(), request);
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        SimpleHttpResponse simpleHttpResponse = new SimpleHttpResponse(fullHttpResponse);
        process(simpleHttpRequest, simpleHttpResponse, channelHandlerContext, startTime);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable cause) {
        logger.error("exceptionCaught error", cause);
        channelHandlerContext.close();
    }

    public void process(SimpleHttpRequest request, SimpleHttpResponse response, ChannelHandlerContext channelHandlerContext, long startTime) {
        String id = StringUtils.replace(UUID.randomUUID().toString(), "-", "");
        ActionHandler handler = dispatcher.getHandler(request.getUrl());
        if (handler == null) {
            response.response("url is error");
            response.getResponse().headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            response.getResponse().headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.getResponse().content().readableBytes());
            channelHandlerContext.writeAndFlush(response.getResponse()).addListener((ChannelFutureListener) future -> {
                future.channel().close();
                if (future.isSuccess()) {
                    serverLogger.info("server handle over id={},url={},cost={} ms", id, request.getUrl(), System.currentTimeMillis() - startTime);
                } else {
                    serverLogger.error("server handle error id={},url={},cost={} ms", id, request.getUrl(), System.currentTimeMillis() - startTime);
                }
            });
        } else {
            serverLogger.info("server handler start id={},url={},header={},parameter={},files={}", id, request.getUrl(), request.getHeaders(), request.getParameters(), request.getFiles());
            handler.execute(channelHandlerContext, request, response, id, startTime);
        }
    }

}
