package com.clubchretien.abonnement.i18n;

import com.owomi.commons.constants.I18nLanguages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

@Component
public class I18nFactory implements I18nLanguages {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    HttpServletRequest request;

    public I18nFactory() {
    }

    public MessageSource getMessageSource() {
        return messageSource;
    }

    public String internationalize(String message) {
        System.out.println("message  " + message);
        System.out.println("this.getLanguage()  " + this.getLanguage());
        System.out.println("Locale.forLanguageTag(this.getLanguage())  " + Locale.forLanguageTag(this.getLanguage()));

        try {
            return messageSource.getMessage(message,
                    null,
                    Locale.forLanguageTag(this.getLanguage()));
        } catch (Exception ignored) {
        }
        return message;
    }

    public String internationalize(String message, Object... objects) {
        try {
            return messageSource.getMessage(message,
                    objects,
                    Locale.forLanguageTag(this.getLanguage()));
        } catch (Exception ignored) {
        }
        return message;
    }

    protected String getLanguage() {
        String lang;
        try {
            lang = this.request.getHeader("Accept-Language");

            if (lang == null || lang.isEmpty()) {
                lang = this.request.getHeader("language");

                if (lang == null || lang.isEmpty()) {
                    lang = FRENCH_LANG_TAG;
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            lang = FRENCH_LANG_TAG;
        }

        LocaleContextHolder.setLocale(Locale.forLanguageTag(lang));
        return lang;
    }
}

