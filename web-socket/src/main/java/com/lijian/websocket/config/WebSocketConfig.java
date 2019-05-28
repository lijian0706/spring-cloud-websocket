package com.lijian.websocket.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Map;

/**
 * Created by lijian on 2018/4/26
 */
@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(StompProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private StompProperties stompProperties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {//配置消息代理  Message Broker 点对点式
        registry.setApplicationDestinationPrefixes("/app") // 配置请求都以/app打头，没有特殊意义，例如：@MessageMapping("/hello")，其实真实路径是/app/hello
                .enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(stompProperties.getServer())
                .setRelayPort(stompProperties.getPort())
                .setClientLogin(stompProperties.getUsername())
                .setClientPasscode(stompProperties.getPassword())
                .setSystemLogin(stompProperties.getUsername())
                .setSystemPasscode(stompProperties.getPassword())
                .setVirtualHost("/")
                .setUserDestinationBroadcast("/topic/greetings")
                .setUserRegistryBroadcast("/topic/greetings");

    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").setHandshakeHandler(defaultHandshakeHandler()).withSockJS();//注册STOMP协议的节点 指定使用SockJS协议,setAllowedOrigins 添加允许跨域访问
    }


    private DefaultHandshakeHandler defaultHandshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                return () -> "lijian";
            }
        };
    }
}
