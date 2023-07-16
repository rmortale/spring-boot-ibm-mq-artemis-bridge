package com.example.springbootibmmq.listener;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;

@Slf4j
@Setter
public class SimpleMessageListener implements MessageListener {

    private JmsTemplate template;
    private String destination;

    @Override
    public void onMessage(Message message) {
        log.info("received message {}", message);
        try {
            template.convertAndSend(destination, message.getBody(String.class));
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
