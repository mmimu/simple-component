package com.mimu.simple.httpserver.core.response;


import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * author: mimu
 * date: 2018/10/21
 */
public class SimpleHttpResponse {
    private static final Logger logger = LoggerFactory.getLogger(SimpleHttpResponse.class);
    private static final String contentEncoding = "UTF-8";
    private static final String contentType = "application/json";
    private FullHttpResponse httpResponse;

    public SimpleHttpResponse(FullHttpResponse response) {
        this.httpResponse = response;
    }

    public FullHttpResponse getResponse() {
        return httpResponse;
    }

    public void response(JSONObject result) {
        response(result.toJSONString());
    }

    public void response(Object result) {
        if (result != null) {
            response(JSONObject.toJSONString(result));
        }
    }

    public void response(String result) {
        response(result, contentType, contentEncoding);
    }

    public void response(String result, String contentType, String encoding) {
        try {
            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            httpResponse.headers().set(HttpHeaderNames.CONTENT_ENCODING, encoding);
            httpResponse.content().writeBytes(result.getBytes(contentEncoding));
        } catch (UnsupportedEncodingException e) {
            logger.error("SimpleHttpResponse error", e);
        }
    }

}
