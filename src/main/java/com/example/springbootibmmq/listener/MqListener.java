package com.example.springbootibmmq.listener;

import com.example.springbootibmmq.sender.Sender;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class MqListener {

//    private final Sender sender;
//
//    public MqListener(Sender sender) {
//        this.sender = sender;
//    }
//
//    @JmsListener(destination = "DEV.QUEUE.1", containerFactory = "qm1JmsListenerContainerFactory")
//    public void receive(String msg, @Header(JmsHeaders.MESSAGE_ID) String messageId) {
//        System.out.println("received " + msg + ", " + messageId);
//        sender.send(msg);
//    }
}
