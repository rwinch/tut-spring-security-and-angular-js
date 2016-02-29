/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package demo.gh93;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ConfigurableObjectInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.web.WebMergedContextConfiguration;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import demo.UiApplication;

/**
 * @author Rob Winch
 *
 */
public class Serializer {


	@Autowired
	OAuth2ClientContext toSerialize;

	@Autowired
	MockHttpServletRequest request;

	@Autowired
	ConfigurableWebApplicationContext wac;

	public byte[] write() throws Exception {
		setup();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try(ObjectOutputStream oos = new ObjectOutputStream(out)) {
			oos.writeObject(toSerialize);
		} finally {
			cleanup();
		}

		return out.toByteArray();
	}

	public Object read(byte[] serialized) throws Exception {

		setup();

		try (ObjectInputStream in = new ConfigurableObjectInputStream(new ByteArrayInputStream(serialized), getClass().getClassLoader())) {
			return in.readObject();
		} finally {
			cleanup();
		}
	}

	/**
	 * Sets up the {@link WebApplicationContext} and the {@link RequestContextHolder}
	 *
	 * @return
	 * @throws Exception
	 */
	public WebApplicationContext setup() throws Exception {
		String[] empty = new String[0];
		SpringApplicationContextLoader loader = new SpringApplicationContextLoader();
		MergedContextConfiguration mcc = new MergedContextConfiguration(getClass(), empty, new Class[] {UiApplication.class}, empty, loader);
		WebMergedContextConfiguration wmcc = new WebMergedContextConfiguration(mcc, "");

		WebApplicationContext wac = (WebApplicationContext) ((SmartContextLoader) wmcc.getContextLoader()).loadContext(wmcc);

		MockServletContext mockServletContext = (MockServletContext) wac.getServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(mockServletContext);
		MockHttpServletResponse response = new MockHttpServletResponse();
		ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);

		RequestContextHolder.setRequestAttributes(servletWebRequest);

		if (wac instanceof ConfigurableApplicationContext) {
			@SuppressWarnings("resource")
			ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) wac;
			ConfigurableListableBeanFactory bf = configurableApplicationContext.getBeanFactory();
			bf.registerResolvableDependency(MockHttpServletResponse.class, response);
			bf.registerResolvableDependency(ServletWebRequest.class, servletWebRequest);

			bf.autowireBean(this);
		}
		return wac;
	}

	/**
	 * Closes the {@link WebApplicationContext} and clears the {@link RequestContextHolder}
	 */
	public void cleanup() {
		wac.close();
		RequestContextHolder.resetRequestAttributes();
	}
}
