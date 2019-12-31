package eu.europeana.sitemap.exceptions;

import eu.europeana.sitemap.config.SitemapConfiguration;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Sends email alerts (to Slack API-exceptions channel) when an important error occurs
 */

@Lazy
@Service
public class MailService {

    private JavaMailSender mailSender;
    private SitemapConfiguration config;

    @Autowired
    public MailService(JavaMailSender mailSender, SitemapConfiguration config) {
        this.mailSender = mailSender;
        this.config = config;
    }

    /**
     * Send an email alert
     * @param errorMessage subject of the email
     * @param t exeception that occurred (as body of the email), can be null
     */
    public void sendErrorEmail(String errorMessage, Throwable t)  {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        try {
            helper.setFrom(config.getMailFrom());
            helper.setTo(config.getMailTo());
            helper.setSubject(errorMessage);
            if (t !=  null) {
                helper.setText(ExceptionUtils.getStackTrace(t));
            }

            mailSender.send(message);
            LogManager.getLogger(MailService.class).info("Email error alert was sent");
        } catch (MessagingException me) {
            LogManager.getLogger(MailService.class).error("Error sending email error alert", me);
        }
    }
}
