package com.clubchretien.abonnement.filters;

import com.clubchretien.abonnement.configs.Router;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static com.owomi.commons.constants.MobileConstant.REQUEST_CONTEXT_ID;

@ControllerAdvice
public class ServiceFilterOut implements ResponseBodyAdvice<Object> {
	Logger logger = LogManager.getLogger(ServiceFilterOut.class);


	@Override
	public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object objet, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
		String chemin = serverHttpRequest.getURI().getPath();
		logger.info(serverHttpRequest.getMethod() + " OUT_REQUEST " + serverHttpRequest.getURI() + " CONTEXT_ID " + serverHttpRequest.getHeaders().get(REQUEST_CONTEXT_ID));
		if (Router.IsDocumentation(chemin) || Router.IsActuator(chemin)) return objet;
		logger.info(serverHttpRequest.getMethod() + " OUT_REQUEST " + serverHttpRequest.getURI() + " CONTEXT_ID " + serverHttpRequest.getHeaders().get(REQUEST_CONTEXT_ID) + " RESPONSE_BODY " + objet);
		return objet;
	}
}
