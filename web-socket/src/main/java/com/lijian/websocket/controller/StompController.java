package com.lijian.websocket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: Lijian
 * @Date: 2019-05-28 15:54
 */
@RestController
public class StompController {


    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/test")
    public void test(String username){
        messagingTemplate.convertAndSendToUser(username, "/topic/greetings", "hello:"+username);
    }
}
