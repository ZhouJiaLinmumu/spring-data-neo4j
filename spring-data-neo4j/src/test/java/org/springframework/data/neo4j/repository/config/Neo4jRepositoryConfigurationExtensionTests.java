/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.repository.config;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * Unit tests for {@link Neo4jRepositoryConfigurationExtension}.
 *
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public class Neo4jRepositoryConfigurationExtensionTests {

	private static final String CUSTOM_SESSION_FACTORY_BEAN_NAME = "mySessionFactory";
	private static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactory";
	private static final String SHARED_SESSION_CREATOR_BEAN_NAME = "sharedSessionCreatorBean";

	public @Rule ExpectedException exception = ExpectedException.none();

	@Test
	public void registersSharedSessionCreatorBeanByDefault() {

		DefaultListableBeanFactory factory = getBeanRegistry(new StandardAnnotationMetadata(Config.class, true));

		Iterable<String> names = Arrays.asList(factory.getBeanDefinitionNames());

		assertThat(names, hasItems(SHARED_SESSION_CREATOR_BEAN_NAME));
	}

	@Test
	public void sessionFactoryBeanNameDefaultsToSessionFactory() {
		BeanDefinition beanDefinition = getBeanRegistry(new StandardAnnotationMetadata(Config.class, true))
				.getBeanDefinition(SHARED_SESSION_CREATOR_BEAN_NAME);

		RuntimeBeanReference sessionFactoryBean = (RuntimeBeanReference) beanDefinition.getConstructorArgumentValues()
				.getIndexedArgumentValue(0, RuntimeBeanReference.class).getValue();

		String sessionFactoryBeanName = sessionFactoryBean.getBeanName();

		assertThat(sessionFactoryBeanName, equalTo(DEFAULT_SESSION_FACTORY_BEAN_NAME));
	}

	@Test
	public void customSessionFactoryBeanNameWillGetUsed() {
		BeanDefinition beanDefinition = getBeanRegistry(new StandardAnnotationMetadata(CustomSessionRefConfig.class, true))
				.getBeanDefinition(SHARED_SESSION_CREATOR_BEAN_NAME);

		RuntimeBeanReference sessionFactoryBean = (RuntimeBeanReference) beanDefinition.getConstructorArgumentValues()
				.getIndexedArgumentValue(0, RuntimeBeanReference.class).getValue();

		String sessionFactoryBeanName = sessionFactoryBean.getBeanName();

		assertThat(sessionFactoryBeanName, equalTo(CUSTOM_SESSION_FACTORY_BEAN_NAME));
	}

	@Test
	public void guardsAgainstNullJavaTypesReturnedFromNeo4jMetaData() throws Exception {

		ApplicationContext context = mock(ApplicationContext.class);
		SessionFactory sessionFactory = mock(SessionFactory.class);
		MetaData metaData = mock(MetaData.class);
		ClassInfo classInfo = mock(ClassInfo.class);

		Set<ClassInfo> managedTypes = Collections.singleton(classInfo);

		when(context.getBean(any(String.class), eq(SessionFactory.class))).thenReturn(sessionFactory);
		when(sessionFactory.metaData()).thenReturn(metaData);
		when(metaData.persistentEntities()).thenReturn(managedTypes);
		when(classInfo.name()).thenReturn("Test");

		Neo4jMappingContextFactoryBean factoryBean = new Neo4jMappingContextFactoryBean();
		factoryBean.setApplicationContext(context);

		factoryBean.createInstance().afterPropertiesSet();
	}

	private DefaultListableBeanFactory getBeanRegistry(final StandardAnnotationMetadata metadata) {
		AnnotationConfigApplicationContext factory = new AnnotationConfigApplicationContext();

		factory.registerBean(DEFAULT_SESSION_FACTORY_BEAN_NAME, SessionFactory.class, "dummypackage");

		RepositoryConfigurationExtension extension = new Neo4jRepositoryConfigurationExtension();
		AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				EnableNeo4jRepositories.class, new PathMatchingResourcePatternResolver(), new StandardEnvironment(), factory);
		extension.registerBeansForRoot(factory, configurationSource);

		return (DefaultListableBeanFactory) factory.getBeanFactory();
	}

	@EnableNeo4jRepositories(considerNestedRepositories = true)
	private static class Config {

	}

	@EnableNeo4jRepositories(sessionFactoryRef = CUSTOM_SESSION_FACTORY_BEAN_NAME)
	private static class CustomSessionRefConfig {

	}
}
