package com.prgrms.ijuju.global.config;

import com.prgrms.ijuju.domain.stock.adv.advancedinvest.handler.AdvancedInvestWebSocketHandler;
import com.prgrms.ijuju.domain.chat.handler.ChatWebSocketHandler;
import com.prgrms.ijuju.global.auth.JwtHandshakeInterceptor;
import com.prgrms.ijuju.global.auth.WebsocketChannelInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.lang.NonNull;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AdvancedInvestWebSocketHandler advancedInvestWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final WebsocketChannelInterceptor webSocketChannelInterceptor;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketConfig(AdvancedInvestWebSocketHandler advancedInvestWebSocketHandler, JwtHandshakeInterceptor jwtHandshakeInterceptor, WebsocketChannelInterceptor webSocketChannelInterceptor, ChatWebSocketHandler chatWebSocketHandler) {
        this.advancedInvestWebSocketHandler = advancedInvestWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.webSocketChannelInterceptor = webSocketChannelInterceptor;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }


    //핸들러 방식
    /*@Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(advancedInvestWebSocketHandler, "/api/v1/advanced-invest")
                .setAllowedOrigins("*");
                //.addInterceptors(jwtHandshakeInterceptor); // 핸들러 URL 등록
    }*/
  
    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws")
                .setAllowedOrigins("*");
    }

    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp", "/api/v1/advanced-invest")

                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(webSocketChannelInterceptor);
    }
}
