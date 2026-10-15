package com.smallerp.mail;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class MailConfig {

    @Bean
    MailSender appMailSender(ObjectProvider<JavaMailSender> javaMailSender,
                          @Value("${app.mail.from:no-reply@smallerp.local}") String from) {
        JavaMailSender smtp = javaMailSender.getIfAvailable();
        return smtp != null ? new SmtpMailSender(smtp, from) : new LoggingMailSender();
    }
}
