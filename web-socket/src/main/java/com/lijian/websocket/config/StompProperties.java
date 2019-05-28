package com.lijian.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by lijian on 2018/11/28
 */
@ConfigurationProperties(prefix = "stomp", ignoreUnknownFields = false)
@Data
public class StompProperties {
    private String server;
    private Integer port;
    private String username;
    private String password;
}
