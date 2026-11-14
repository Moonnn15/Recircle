package com.recircle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
    "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
})
@EnableScheduling
public class ReCircleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReCircleApplication.class, args);
    }
}
