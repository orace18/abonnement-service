
package com.clubchretien.abonnement.xss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * User: dev.bscako
 * Date: 03/06/2021 Time: 12:46
 */

@Slf4j
@RequiredArgsConstructor
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XSSFilter implements Filter {

    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

    private void urlencoded(ServletRequest request) {
        log.debug("url encoded all parameters {}", request.getParameterMap().keySet());
        for (String key : request.getParameterMap().keySet()) {
            log.debug("url encoded {} {}", key, XSSUtils.PrettyPrintJson(request.getParameter(key)));
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String contentType = request.getContentType();
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (StringUtils.isNotBlank(contentType) && contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            this.urlencoded(request);
            chain.doFilter(request, response);
            return;
        }
        if (StringUtils.isNotBlank(contentType) && contentType.contains(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            MultipartHttpServletRequest multipart = null;
            try {
                multipart = new CommonsMultipartResolver().resolveMultipart((HttpServletRequest) request);
            } catch (MultipartException e) {
                log.debug(" no multipart request {}", e.getLocalizedMessage());
                handlerExceptionResolver.resolveException(
                        (HttpServletRequest) request, (HttpServletResponse) response,
                        null, e
                );
                return;
            }
            XSSRequestWrapper wrappedRequest = new XSSRequestWrapper(multipart);
            chain.doFilter(wrappedRequest, response);
            return;
        }
        XSSRequestWrapper wrappedRequest = new XSSRequestWrapper((HttpServletRequest) request);
        try {
            String body = IOUtils.toString(wrappedRequest.getReader());
            if (!StringUtils.isBlank(body)) {
                log.debug("app/json body request {} ", body);
                body = XSSUtils.stripXSS(body);
                byte[] bytes = null;
                if (body != null) {
                    bytes = body.getBytes();
                }
                wrappedRequest.resetInputStream(bytes);
            }
        } catch (Exception ex) {
            handlerExceptionResolver.resolveException(
                    (HttpServletRequest) request, (HttpServletResponse) response,
                    null, ex
            );
            return;
        }
        log.debug("xss filter next next {}", ((HttpServletRequest) request).getRequestURI());
        chain.doFilter(wrappedRequest, response);
    }

    public Cookie[] resetCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        Map<String, String> map = new HashMap<>();
        if (cookies != null && cookies.length != 0) {
            for (Cookie cookie : cookies) {
                String value = cookie.getValue();
                if (value != null) {
                    String deleteValue = XSSUtils.stripXSS(value);
                    if (!value.equals(deleteValue)) {
                        map.put(cookie.getName(), cookie.getDomain());
                    }
                }
            }

//            for (Map.Entry<String, String> cookie : map.entrySet()) {
//                CookieUtil.removeCookie(request, response, cookie.getValue(), cookie.getKey());
//            }
        }
        return cookies;
    }

}




