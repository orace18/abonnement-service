package com.clubchretien.abonnement.i18n;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

import static com.owomi.commons.constants.I18nLanguages.FRENCH_LANG_TAG;

@Component
public class I18nVelocityFactory extends com.owomi.velocity.i18n.I18nVelocityFactory<MessageSource, HttpServletRequest> {

    @Autowired
    MessageSource messageSource;
    @Autowired
    HttpServletRequest httpServletRequest;

    @Override
    public String internationalizeWithTabParams(String message, Object[] objects) {
        try {
            return messageSource.getMessage(message,
                    objects,
                    Locale.forLanguageTag(this.getLanguage()));
        } catch (Exception ignored) {
        }
        return message;
    }

    @Override
    public String getLanguage() {
        String lang;
        try {
            lang = this.httpServletRequest.getHeader("Accept-Language");

            if (lang == null || lang.isEmpty()) {
                lang = this.httpServletRequest.getHeader("language");

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
