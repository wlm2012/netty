/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.http.websocketx.extensions;

import static io.netty.util.internal.ObjectUtil.checkNonEmpty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

/**
 * This handler negotiates and initializes the WebSocket Extensions.
 *
 * It negotiates the extensions based on the client desired order,
 * ensures that the successfully negotiated extensions are consistent between them,
 * and initializes the channel pipeline with the extension decoder and encoder.
 *
 * Find a basic implementation for compression extensions at
 * <tt>io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler</tt>.
 */
public class WebSocketServerExtensionHandler extends ChannelDuplexHandler {

    private final List<WebSocketServerExtensionHandshaker> extensionHandshakers;

    private final Queue<List<WebSocketServerExtension>> validExtensions =
            new ArrayDeque<List<WebSocketServerExtension>>(4);

    /**
     * Constructor
     *
     * @param extensionHandshakers
     *      The extension handshaker in priority order. A handshaker could be repeated many times
     *      with fallback configuration.
     */
    public WebSocketServerExtensionHandler(WebSocketServerExtensionHandshaker... extensionHandshakers) {
        this.extensionHandshakers = Arrays.asList(checkNonEmpty(extensionHandshakers, "extensionHandshakers"));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            List<WebSocketServerExtension> validExtensionsList = null;
            HttpRequest request = (HttpRequest) msg;

            if (WebSocketExtensionUtil.isWebsocketUpgrade(request.headers())) {
                String extensionsHeader = request.headers().getAsString(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS);

                if (extensionsHeader != null) {
                    List<WebSocketExtensionData> extensions =
                            WebSocketExtensionUtil.extractExtensions(extensionsHeader);
                    int rsv = 0;

                    for (WebSocketExtensionData extensionData : extensions) {
                        Iterator<WebSocketServerExtensionHandshaker> extensionHandshakersIterator =
                                extensionHandshakers.iterator();
                        WebSocketServerExtension validExtension = null;

                        while (validExtension == null && extensionHandshakersIterator.hasNext()) {
                            WebSocketServerExtensionHandshaker extensionHandshaker =
                                    extensionHandshakersIterator.next();
                            validExtension = extensionHandshaker.handshakeExtension(extensionData);
                        }

                        if (validExtension != null && ((validExtension.rsv() & rsv) == 0)) {
                            if (validExtensionsList == null) {
                                validExtensionsList = new ArrayList<WebSocketServerExtension>(1);
                            }
                            rsv = rsv | validExtension.rsv();
                            validExtensionsList.add(validExtension);
                        }
                    }
                }
            }

            if (validExtensionsList == null) {
                validExtensionsList = Collections.emptyList();
            }
            validExtensions.offer(validExtensionsList);
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpResponse) {
            List<WebSocketServerExtension> validExtensionsList = validExtensions.poll();
            HttpResponse httpResponse = (HttpResponse) msg;
            //checking the status is faster than looking at headers
            //so we do this first
            if (HttpResponseStatus.SWITCHING_PROTOCOLS.equals(httpResponse.status())) {
                handlePotentialUpgrade(ctx, promise, httpResponse, validExtensionsList);
            }
        }

        super.write(ctx, msg, promise);
    }

    private void handlePotentialUpgrade(final ChannelHandlerContext ctx,
                                        ChannelPromise promise, HttpResponse httpResponse,
                                        final List<WebSocketServerExtension> validExtensionsList) {
        HttpHeaders headers = httpResponse.headers();

        if (WebSocketExtensionUtil.isWebsocketUpgrade(headers)) {
            if (validExtensionsList != null && !validExtensionsList.isEmpty()) {
                String headerValue = headers.getAsString(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS);
                List<WebSocketExtensionData> extraExtensions =
                  new ArrayList<WebSocketExtensionData>(extensionHandshakers.size());
                for (WebSocketServerExtension extension : validExtensionsList) {
                    extraExtensions.add(extension.newReponseData());
                }
                String newHeaderValue = WebSocketExtensionUtil
                  .computeMergeExtensionsHeaderValue(headerValue, extraExtensions);
                promise.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {
                            for (WebSocketServerExtension extension : validExtensionsList) {
                                WebSocketExtensionDecoder decoder = extension.newExtensionDecoder();
                                WebSocketExtensionEncoder encoder = extension.newExtensionEncoder();
                                String name = ctx.name();
                                ctx.pipeline()
                                    .addAfter(name, decoder.getClass().getName(), decoder)
                                    .addAfter(name, encoder.getClass().getName(), encoder);
                            }
                        }
                    }
                });

                if (newHeaderValue != null) {
                    headers.set(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS, newHeaderValue);
                }
            }

            promise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        ctx.pipeline().remove(WebSocketServerExtensionHandler.this);
                    }
                }
            });
        }
    }
}
