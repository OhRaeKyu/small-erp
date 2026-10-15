package com.smallerp.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 로컬·테스트용: 실제로 보내지 않고 내용을 로그로 남긴다. */
public class LoggingMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

    @Override
    public void send(MailMessage message) {
        log.info("[메일 미발송: SMTP 미설정] to={} subject={}\n{}", message.to(), message.subject(), message.body());
    }
}
