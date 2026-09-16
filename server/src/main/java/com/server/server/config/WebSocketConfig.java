package com.server.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer
{
    private final com.server.server.matchmaking.RoomRegistry rooms;
    private final String[] allowedOrigins;
    public WebSocketConfig(com.server.server.matchmaking.RoomRegistry rooms,
        @org.springframework.beans.factory.annotation.Value("${game.allowed-origins:http://localhost:8080}") String origins) {
        this.rooms = rooms;
        this.allowedOrigins = java.util.Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        if (java.util.Arrays.stream(allowedOrigins).anyMatch(s -> s.contains("*")))
            throw new IllegalArgumentException("game.allowed-origins must contain explicit origins, not wildcards");
    }
    @Override public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new GameChannelInterceptor(rooms));
    }
    @Override public void configureWebSocketTransport(org.springframework.web.socket.config.annotation.WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(16384).setSendBufferSizeLimit(262144).setSendTimeLimit(10000);
    }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry)
    {
        registry.setPreserveReceiveOrder(true);
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);

        registry.addEndpoint("/ws-sockjs")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry)
    {
        registry.enableSimpleBroker("/match", "/queue").setHeartbeatValue(new long[]{5000,5000}).setTaskScheduler(gameHeartbeats());
        registry.setPreservePublishOrder(true);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler gameHeartbeats() {
        var scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1); scheduler.setDaemon(true); scheduler.setThreadNamePrefix("game-heartbeat-");
        return scheduler;
    }


}
