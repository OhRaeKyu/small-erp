package com.smallerp.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 승인·반려 트랜잭션이 커밋된 뒤 별도 스레드에서 메일을 보낸다.
 * 발송이 실패해도 이미 커밋된 승인·반려는 그대로 두고 로그만 남긴다.
 */
@Component
public class MailEventListener {

    private static final Logger log = LoggerFactory.getLogger(MailEventListener.class);

    private final MailSender mailSender;

    public MailEventListener(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MailRequestedEvent event) {
        try {
            mailSender.send(event.message());
        } catch (RuntimeException e) {
            log.error("메일 발송 실패 to={} subject={}", event.message().to(), event.message().subject(), e);
        }
    }
}
