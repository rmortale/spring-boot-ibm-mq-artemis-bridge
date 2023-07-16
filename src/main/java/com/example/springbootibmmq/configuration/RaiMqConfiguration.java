package com.example.springbootibmmq.configuration;

import com.example.springbootibmmq.listener.SimpleMessageListener;
import com.ibm.mq.jakarta.jms.MQConnectionFactory;
import com.ibm.mq.spring.boot.MQConfigurationProperties;
import com.ibm.mq.spring.boot.MQConnectionFactoryCustomizer;
import com.ibm.mq.spring.boot.MQConnectionFactoryFactory;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import java.util.List;

@Configuration
@Slf4j
public class RaiMqConfiguration implements JmsListenerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(RaiMqConfiguration.class);

    @Autowired
    private ForwarderProperties forwarderProperties;
    @Autowired
    private ApplicationContext ac;

    @Bean
    @ConfigurationProperties("qm1")
    public MQConfigurationProperties qm1ConfigProperties() {
        MQConfigurationProperties props = new MQConfigurationProperties();
        return props;
    }

    @Bean
    public CachingConnectionFactory amqConnectionFactory(
            @Value("${raifwd.artemis.broker-url}") String url,
            @Value("${raifwd.artemis.user}") String user,
            @Value("${raifwd.artemis.password}") String pw) {
        return new CachingConnectionFactory(new ActiveMQConnectionFactory(url, user, pw));
    }

    @Bean
    public CachingConnectionFactory qm1ConnectionFactory(
            @Qualifier("qm1ConfigProperties") MQConfigurationProperties properties,
            ObjectProvider<List<MQConnectionFactoryCustomizer>> factoryCustomizers) {
        return new CachingConnectionFactory(new MQConnectionFactoryFactory(properties,
                factoryCustomizers.getIfAvailable()).createConnectionFactory(MQConnectionFactory.class));
    }

    @Bean
    public JmsListenerContainerFactory<?> qm1JmsListenerContainerFactory(
            @Qualifier("qm1ConnectionFactory") ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        return factory;
    }

    @Bean
    public JmsListenerContainerFactory<?> amqJmsListenerContainerFactory(
            @Qualifier("amqConnectionFactory") ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        return factory;
    }

    @Bean
    public JmsTemplate wmqTemplate(@Qualifier("qm1ConnectionFactory") ConnectionFactory connectionFactory) {
        JmsTemplate t = new JmsTemplate(connectionFactory);
        t.setSessionTransacted(true);
        return t;
    }

    @Bean
    public JmsTemplate amqTemplate(@Qualifier("amqConnectionFactory") ConnectionFactory connectionFactory) {
        JmsTemplate t = new JmsTemplate(connectionFactory);
        t.setSessionTransacted(true);
        return t;
    }

    @Override
    public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
        // configure from WMQ to AMQ
        for (QueueMapping mapping : forwarderProperties.getWmqToAmq()) {
            SimpleJmsListenerEndpoint endpoint = createEndpoint(mapping.getSourceQueue(), mapping.getTargetQueue(),
                    "amqTemplate");
            registrar.registerEndpoint(endpoint, ac.getBean("qm1JmsListenerContainerFactory",
                    JmsListenerContainerFactory.class));
            log.info("registered JMS listener for WMQ on destination {}", mapping.getSourceQueue());
        }

        // configure from AMQ to WMQ
        for (QueueMapping mapping : forwarderProperties.getAmqToWmq()) {
            SimpleJmsListenerEndpoint endpoint = createEndpoint(mapping.getSourceQueue(), mapping.getTargetQueue(),
                    "wmqTemplate");
            registrar.registerEndpoint(endpoint, ac.getBean("amqJmsListenerContainerFactory",
                    JmsListenerContainerFactory.class));
            log.info("registered JMS listener for AMQ on destination {}", mapping.getSourceQueue());
        }
    }

    private SimpleJmsListenerEndpoint createEndpoint(String sourceQueue, String targetQueue, String templateName) {
        SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
        endpoint.setId(sourceQueue);
        endpoint.setDestination(sourceQueue);
        SimpleMessageListener listener = createListener(targetQueue, templateName);
        endpoint.setMessageListener(listener);
        return endpoint;
    }

    private SimpleMessageListener createListener(String targetQueue, String templateName) {
        SimpleMessageListener listener = new SimpleMessageListener();
        listener.setDestination(targetQueue);
        listener.setTemplate(ac.getBean(templateName, JmsTemplate.class));
        return listener;
    }
}
