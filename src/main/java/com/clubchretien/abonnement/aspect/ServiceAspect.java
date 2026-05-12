package com.clubchretien.abonnement.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

@Aspect
@Configuration
public class ServiceAspect {
	private Logger logger = LogManager.getLogger(this.getClass());

	@Before(value = "execution(* com.clubchretien.social.service.services.*.*(..)))")
	public void methodLogger(JoinPoint joinPoint) {
		logger.info("Enters on method: {}, allParameters {}, requestContext {}",
				joinPoint.getSignature().getName(),
				Arrays.stream(joinPoint.getArgs()).filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(","))
		);
	}

	@After(value = "execution(* com.clubchretien.social.service.services.*.*(..))")
	public void afterReturning(JoinPoint joinPoint) {
		logger.info("Exits on method: {}, requestContext {}",
				joinPoint.getSignature().getName(),
				Arrays.stream(joinPoint.getArgs()).filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(","))
		);
	}
}
