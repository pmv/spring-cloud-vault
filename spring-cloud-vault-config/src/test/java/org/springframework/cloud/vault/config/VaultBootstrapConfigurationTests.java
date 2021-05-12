/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.cloud.vault.config;

import java.time.Duration;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.client.RestTemplateFactory;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.SslConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VaultBootstrapConfiguration}.
 *
 * @author Mark Paluch
 */
public class VaultBootstrapConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(VaultBootstrapConfiguration.class));

	@Test
	public void shouldConfigureWithoutAuthentication() {

		this.contextRunner.withPropertyValues("spring.cloud.vault.kv.enabled=false",
				"spring.cloud.vault.authentication=NONE", "spring.cloud.bootstrap.enabled=true").run(context -> {

					assertThat(context).doesNotHaveBean(SessionManager.class);
					assertThat(context).doesNotHaveBean(ClientAuthentication.class);
					assertThat(context).hasSingleBean(VaultTemplate.class);
					assertThat(context).hasSingleBean(RestTemplateFactory.class);
				});
	}

	@Test
	public void shouldApplySslSettings() {

		this.contextRunner.withPropertyValues("spring.cloud.vault.kv.enabled=false",
				"spring.cloud.vault.authentication=NONE", "spring.cloud.bootstrap.enabled=true",
				"spring.cloud.vault.ssl.enabled-protocols=TLSv1.2,TLSv1.3",
				"spring.cloud.vault.ssl.enabled-cipher-suites=one,two").run(context -> {

					VaultProperties properties = context.getBean(VaultProperties.class);

					SslConfiguration sslConfiguration = VaultConfiguration.createSslConfiguration(properties.getSsl());

					assertThat(sslConfiguration.getEnabledProtocols()).containsExactly("TLSv1.2", "TLSv1.3");
					assertThat(sslConfiguration.getEnabledCipherSuites()).containsExactly("one", "two");
				});
	}

	@Test
	public void shouldDisableSessionManagement() {

		this.contextRunner
				.withPropertyValues("spring.cloud.vault.kv.enabled=false", "spring.cloud.vault.token=foo",
						"spring.cloud.vault.session.lifecycle.enabled=false", "spring.cloud.bootstrap.enabled=true")
				.run(context -> {

					SessionManager bean = context.getBean(SessionManager.class);
					assertThat(bean).isExactlyInstanceOf(SimpleSessionManager.class);
				});
	}

	@Test
	public void shouldConfigureSessionManagement() {

		this.contextRunner.withPropertyValues("spring.cloud.vault.kv.enabled=false", "spring.cloud.vault.token=foo",
				"spring.cloud.vault.session.lifecycle.refresh-before-expiry=11s",
				"spring.cloud.vault.session.lifecycle.expiry-threshold=12s", "spring.cloud.bootstrap.enabled=true")
				.run(context -> {

					SessionManager bean = context.getBean(SessionManager.class);

					Object refreshTrigger = ReflectionTestUtils.getField(bean, "refreshTrigger");

					assertThat(refreshTrigger).hasFieldOrPropertyWithValue("duration", Duration.ofSeconds(11))
							.hasFieldOrPropertyWithValue("expiryThreshold", Duration.ofSeconds(12));
				});
	}

}
