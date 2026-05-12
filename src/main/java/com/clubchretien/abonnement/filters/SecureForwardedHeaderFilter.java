package com.clubchretien.abonnement.filters;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.ForwardedHeaderFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class SecureForwardedHeaderFilter extends ForwardedHeaderFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		super.doFilterInternal(request, response, new SecureRequestWrappedFilterChain(filterChain));
	}

	private static class SecureRequestWrappedFilterChain implements FilterChain {
		private final FilterChain delegate;

		public SecureRequestWrappedFilterChain(FilterChain delegate) {
			this.delegate = delegate;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) throws ServletException, IOException {
			delegate.doFilter(new SecureRequest((HttpServletRequest) request), response);
		}
	}

	private static class SecureRequest extends HttpServletRequestWrapper {
		public SecureRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public boolean isSecure() {
			return true;
		}

		@Override
		public String getScheme() {
			return "https";
		}

		@Override
		public StringBuffer getRequestURL() {
			return new StringBuffer(super.getRequestURL().toString().replaceAll("^http://", "https://"));
		}
	}
}
