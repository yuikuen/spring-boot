/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.reactive;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.CloudFoundryAuthorizationException;
import org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.SecurityResponse;
import org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint.Token;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Security interceptor to validate the cloud foundry token.
 *
 * @author Madhura Bhave
 */
class SecurityInterceptor {

	private static final Log logger = LogFactory.getLog(SecurityInterceptor.class);

	private final @Nullable TokenValidator tokenValidator;

	private final @Nullable SecurityService cloudFoundrySecurityService;

	private final @Nullable String applicationId;

	private static final Mono<SecurityResponse> SUCCESS = Mono.just(SecurityResponse.success());

	SecurityInterceptor(@Nullable TokenValidator tokenValidator, @Nullable SecurityService cloudFoundrySecurityService,
			@Nullable String applicationId) {
		this.tokenValidator = tokenValidator;
		this.cloudFoundrySecurityService = cloudFoundrySecurityService;
		this.applicationId = applicationId;
	}

	Mono<SecurityResponse> preHandle(ServerWebExchange exchange, String id) {
		ServerHttpRequest request = exchange.getRequest();
		if (CorsUtils.isPreFlightRequest(request)) {
			return SUCCESS;
		}
		if (!StringUtils.hasText(this.applicationId)) {
			return Mono.error(new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
					"Application id is not available"));
		}
		if (this.cloudFoundrySecurityService == null || this.tokenValidator == null) {
			return Mono.error(new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
					"Cloud controller URL is not available"));
		}
		return check(exchange, id).then(SUCCESS).doOnError(this::logError).onErrorResume(this::getErrorResponse);
	}

	private void logError(Throwable ex) {
		logger.error(ex.getMessage(), ex);
	}

	private Mono<Void> check(ServerWebExchange exchange, String id) {
		Assert.state(this.tokenValidator != null, "'tokenValidator' must not be null");
		Assert.state(this.cloudFoundrySecurityService != null, "'cloudFoundrySecurityService' must not be null");
		Assert.state(this.applicationId != null, "'applicationId' must not be null");
		try {
			Token token = getToken(exchange.getRequest());
			return this.tokenValidator.validate(token)
				.then(this.cloudFoundrySecurityService.getAccessLevel(token.toString(), this.applicationId))
				.filter((accessLevel) -> accessLevel.isAccessAllowed(id))
				.switchIfEmpty(
						Mono.error(new CloudFoundryAuthorizationException(Reason.ACCESS_DENIED, "Access denied")))
				.doOnSuccess((accessLevel) -> exchange.getAttributes().put("cloudFoundryAccessLevel", accessLevel))
				.then();
		}
		catch (CloudFoundryAuthorizationException ex) {
			return Mono.error(ex);
		}
	}

	private Mono<SecurityResponse> getErrorResponse(Throwable throwable) {
		if (throwable instanceof CloudFoundryAuthorizationException cfException) {
			return Mono.just(new SecurityResponse(cfException.getStatusCode(),
					"{\"security_error\":\"" + cfException.getMessage() + "\"}"));
		}
		return Mono.just(new SecurityResponse(HttpStatus.INTERNAL_SERVER_ERROR, throwable.getMessage()));
	}

	private Token getToken(ServerHttpRequest request) {
		String authorization = request.getHeaders().getFirst("Authorization");
		String bearerPrefix = "bearer ";
		if (authorization == null || !authorization.toLowerCase(Locale.ENGLISH).startsWith(bearerPrefix)) {
			throw new CloudFoundryAuthorizationException(Reason.MISSING_AUTHORIZATION,
					"Authorization header is missing or invalid");
		}
		return new Token(authorization.substring(bearerPrefix.length()));
	}

}
