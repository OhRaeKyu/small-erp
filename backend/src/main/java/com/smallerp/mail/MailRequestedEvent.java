package com.smallerp.mail;

/** 트랜잭션이 커밋된 뒤에 보낼 메일 */
public record MailRequestedEvent(MailMessage message) {
}
