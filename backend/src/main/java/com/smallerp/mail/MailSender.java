package com.smallerp.mail;

/** 메일 발송. SMTP 설정이 있으면 SMTP로, 없으면 로그로만 남긴다. */
public interface MailSender {

    void send(MailMessage message);
}
