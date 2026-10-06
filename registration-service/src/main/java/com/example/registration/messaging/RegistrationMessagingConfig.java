package com.example.registration.messaging;

import com.example.shared.messaging.SharedMessagingConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Import(SharedMessagingConfig.class)
public class RegistrationMessagingConfig
{
}
