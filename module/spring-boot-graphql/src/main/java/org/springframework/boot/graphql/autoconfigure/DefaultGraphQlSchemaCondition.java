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

package org.springframework.boot.graphql.autoconfigure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.util.Assert;

/**
 * {@link Condition} that checks whether a GraphQL schema has been defined in the
 * application. This is looking for:
 * <ul>
 * <li>schema files in the {@link GraphQlProperties configured locations}</li>
 * <li>or {@link GraphQlSourceBuilderCustomizer} beans</li>
 * <li>or a {@link GraphQlSource} bean</li>
 * </ul>
 *
 * @author Brian Clozel
 * @see ConditionalOnGraphQlSchema
 */
class DefaultGraphQlSchemaCondition extends SpringBootCondition implements ConfigurationCondition {

	@Override
	public ConfigurationCondition.ConfigurationPhase getConfigurationPhase() {
		return ConfigurationCondition.ConfigurationPhase.REGISTER_BEAN;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		boolean match = false;
		List<ConditionMessage> messages = new ArrayList<>(2);
		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnGraphQlSchema.class);
		Binder binder = Binder.get(context.getEnvironment());
		GraphQlProperties.Schema schema = binder.bind("spring.graphql.schema", GraphQlProperties.Schema.class)
			.orElse(new GraphQlProperties.Schema());
		ResourcePatternResolver resourcePatternResolver = ResourcePatternUtils
			.getResourcePatternResolver(context.getResourceLoader());
		List<Resource> schemaResources = resolveSchemaResources(resourcePatternResolver, schema.getLocations(),
				schema.getFileExtensions());
		if (!schemaResources.isEmpty()) {
			match = true;
			messages.add(message.found("schema", "schemas").items(ConditionMessage.Style.QUOTE, schemaResources));
		}
		else {
			messages.add(message.didNotFind("schema files in locations")
				.items(ConditionMessage.Style.QUOTE, Arrays.asList(schema.getLocations())));
		}
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		Assert.state(beanFactory != null, "'beanFactory' must not be null");
		String[] customizerBeans = beanFactory.getBeanNamesForType(GraphQlSourceBuilderCustomizer.class, false, false);
		if (customizerBeans.length != 0) {
			match = true;
			messages.add(message.found("customizer", "customizers").items(Arrays.asList(customizerBeans)));
		}
		else {
			messages.add((message.didNotFind("GraphQlSourceBuilderCustomizer").atAll()));
		}
		String[] graphQlSourceBeanNames = beanFactory.getBeanNamesForType(GraphQlSource.class, false, false);
		if (graphQlSourceBeanNames.length != 0) {
			match = true;
			messages.add(message.found("GraphQlSource").items(Arrays.asList(graphQlSourceBeanNames)));
		}
		else {
			messages.add((message.didNotFind("GraphQlSource").atAll()));
		}
		return new ConditionOutcome(match, ConditionMessage.of(messages));
	}

	private List<Resource> resolveSchemaResources(ResourcePatternResolver resolver, String[] locations,
			String[] extensions) {
		List<Resource> resources = new ArrayList<>();
		for (String location : locations) {
			for (String extension : extensions) {
				resources.addAll(resolveSchemaResources(resolver, location + "*" + extension));
			}
		}
		return resources;
	}

	private List<Resource> resolveSchemaResources(ResourcePatternResolver resolver, String pattern) {
		try {
			return Arrays.asList(resolver.getResources(pattern));
		}
		catch (IOException ex) {
			return Collections.emptyList();
		}
	}

}
