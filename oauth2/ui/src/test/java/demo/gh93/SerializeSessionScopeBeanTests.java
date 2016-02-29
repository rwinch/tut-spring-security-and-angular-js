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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;
import org.springframework.util.ReflectionUtils;

/**
 *
 * @author Rob Winch
 *
 */
public class SerializeSessionScopeBeanTests {

	@Test
	public void serializeDeserialize() throws Exception {
		// we must repeat the test a few times since sometimes the test will
		// pass
		for(int i=0;i<10;i++) {
			performSerializeDeserialize();
		}
	}

	public void performSerializeDeserialize() throws Exception {
		URLClassLoader classLoader = (URLClassLoader) SerializeSessionScopeBeanTests.class.getClassLoader();

		URLClassLoader writeLoader = createIsolatedClassLoader(classLoader);

		Class<?> writeClass = writeLoader.loadClass(Serializer.class.getName());
		Object writeInstance = writeClass.newInstance();
		Method writeMethod = ReflectionUtils.findMethod(writeClass, "write");
		byte[] serialized = (byte[]) ReflectionUtils.invokeMethod(writeMethod, writeInstance);

		URLClassLoader readLoader = createIsolatedClassLoader(classLoader);

		Class<?> readClass = readLoader.loadClass(Serializer.class.getName());
		Object readInstance = readClass.newInstance();
		Method readMethod = ReflectionUtils.findMethod(readClass, "read", new Class[] { byte[].class});
		Object deserialized = ReflectionUtils.invokeMethod(readMethod, readInstance, serialized);

		assertNotNull(deserialized);
		assertThat(deserialized.getClass().getName(), containsString("Proxy"));
	}

	/**
	 * Creates an isolated classloader to ensure that the static field
	 * {@code DefaultListableBeanFactory#serializableFactories} is not cached.
	 *
	 * @param originalClassLoader the {@link ClassLoader} to copy
	 * @return
	 */
	private URLClassLoader createIsolatedClassLoader(URLClassLoader originalClassLoader) {
		URL[] urls = originalClassLoader.getURLs();
		ClassLoader parent = originalClassLoader.getParent();

		URLClassLoader isolatedClassLoader = new URLClassLoader(urls, parent);
		Thread.currentThread().setContextClassLoader(isolatedClassLoader);
		return isolatedClassLoader;
	}
}
