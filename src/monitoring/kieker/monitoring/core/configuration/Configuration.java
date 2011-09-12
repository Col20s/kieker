/***************************************************************************
 * Copyright 2011 by
 *  + Christian-Albrechts-University of Kiel
 *    + Department of Computer Science
 *      + Software Engineering Group 
 *  and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package kieker.monitoring.core.configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

import kieker.monitoring.core.controller.MonitoringController;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Andre van Hoorn, Jan Waller
 */
public final class Configuration extends Properties implements Keys {
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(Configuration.class);
	
	/*
	 * factory methods
	 */
	
	/**
	 * Creates the configuration for the singleton controller instance. Note
	 * that the {@link Properties} returned by this method are not a
	 * singleton instance, i.e., each call returns an equal but not same set of
	 * {@link Properties}.
	 * 
	 * @return the configuration for the singleton controller
	 */
	public final static Configuration createSingletonConfiguration() {
		Configuration.log.debug("Searching for JVM argument '" + CUSTOM_PROPERTIES_LOCATION_JVM + "' ...");
		// Searching for configuration file location passed to JVM
		String configurationFile = System.getProperty(CUSTOM_PROPERTIES_LOCATION_JVM);
		final Configuration loadConfiguration;
		final Configuration defaultConfiguration = defaultConfiguration();
		// ignore default default-name and set to KIEKER-SINGLETON
		defaultConfiguration.setProperty(CONTROLLER_NAME, "KIEKER-SINGLETON");
		if (configurationFile != null) {
			Configuration.log.info("Loading configuration from JVM-specified location: '" + configurationFile + "'");
			loadConfiguration = loadConfigurationFromFile(configurationFile, defaultConfiguration);
		} else {
			// No JVM property; Trying to find configuration file in classpath
			configurationFile = CUSTOM_PROPERTIES_LOCATION_CLASSPATH;
			Configuration.log.info("Loading properties from properties file in classpath: '" + configurationFile + "'");
			loadConfiguration = loadConfigurationFromResource(configurationFile, defaultConfiguration);
		}
		// 1.JVM-params   ->  2.properties file  ->  3.default properties file
		final Configuration configuration = getSystemPropertiesStartingWith(PREFIX, loadConfiguration);
		return configuration;
	}

	/**
	 * Returns an empty properties map with a fallback on the default configuration.
	 * 
	 * @return default configuration
	 */
	public final static Configuration createDefaultConfiguration() {
		return new Configuration(defaultConfiguration());
	}
	
	/**
	 * Creates a new configuration based on the given properties file with fallback on the default values.
	 * If the file does not exists, a warning is logged and an empty configuration with fallback on
	 * the default configuration is returned.
	 * 
	 * @param configurationFile
	 * @return the created Configuration
	 */
	public final static Configuration createConfigurationFromFile(final String configurationFile) {
		return loadConfigurationFromFile(configurationFile, defaultConfiguration());
	}
		
	/**
	 * Returns a properties map with the default configuration.
	 * 
	 * @return
	 */
	private final static Configuration defaultConfiguration() {
		return loadConfigurationFromResource(DEFAULT_PROPERTIES_LOCATION_CLASSPATH, null);
	}
	
	/**
	 * Returns the properties loaded from file propertiesFn with fallback on the default values.
	 * If the file does not exists, a warning is logged and an empty configuration with fallback on
	 * the default configuration is returned.
	 * 
	 * @param propertiesFn
	 * @param defaultValues
	 * @return
	 */
	private final static Configuration loadConfigurationFromFile(final String propertiesFn, final Configuration defaultValues) {
		try {
			final Configuration properties = new Configuration(defaultValues);
			properties.load(new FileInputStream(propertiesFn));
			return properties;
		} catch (final FileNotFoundException ex) {
			Configuration.log.warn("File '" + propertiesFn + "' not found");
		} catch (final Exception ex) {
			Configuration.log.error("Error reading file '" + propertiesFn + "'", ex);
		}
		return new Configuration(defaultValues);
	}

	/**
	 * Returns the properties loaded from the resource name with fallback on the default values.
	 * If the file does not exists, a warning is logged and an empty configuration with fallback on
	 * the default configuration is returned.
	 * 
	 * @param propertiesFn
	 * @param defaultValues
	 * @return
	 */
	private final static Configuration loadConfigurationFromResource(final String propertiesFn, final Configuration defaultValues) {
		final InputStream is = MonitoringController.class.getClassLoader().getResourceAsStream(propertiesFn);
		if (is == null) {
			Configuration.log.warn("File '" + propertiesFn + "' not found in classpath");
		} else {
			try {
				final Configuration properties = new Configuration(defaultValues);
				properties.load(is);
				return properties;
			} catch (final Exception ex) {
				Configuration.log.error("Error reading file '" + propertiesFn + "'", ex);
			}
		}
		return new Configuration(defaultValues);
	}
	
	/**
	 * Returns the system properties starting with prefix.
	 * 
	 * @param prefix
	 * @param defaultValues
	 * @return
	 */
	private final static Configuration getSystemPropertiesStartingWith(final String prefix, final Configuration defaultValues) {
		final Configuration configuration = new Configuration(defaultValues);
		final Properties properties = System.getProperties(); 
		for (String property : properties.stringPropertyNames()) {
			if (property.startsWith(prefix)) {
				configuration.setProperty(property, properties.getProperty(property));
			}
		}
		return configuration;
	}
	
	/*
	 * member methods
	 */
	
	private Configuration(Configuration defaultValues) {
		super(defaultValues);
	}
	
	/**
	 * You should know what you do if you use this method!
	 * Currently it is used for a (dirty) hack to implement writers.
	 * 
	 * @param defaultProperties
	 * @throws IllegalAccessException 
	 */
	public final void setDefaultProperties(Properties defaultProperties) throws IllegalAccessException {
		if (this.defaults == null) {
			this.defaults = defaultProperties;
		} else if (defaultProperties != null){
			throw new IllegalAccessException();
		}
	}
	
	public final Configuration getPropertiesStartingWith(final String prefix) {
		final Configuration configuration = new Configuration(null);
		for (String property : this.stringPropertyNames()) {
			if (property.startsWith(prefix)) {
				configuration.setProperty(property, this.getProperty(property));
			}
		}
		return configuration;
	}
	
	public final String getStringProperty(final String key) {
		final String s = this.getProperty(key);
		return (s == null) ? "" : s;
	}
	
	public final boolean getBooleanProperty(final String key) {
		return Boolean.parseBoolean(this.getStringProperty(key));
	}
	
	public final int getIntProperty(final String key) {
		final String s = getStringProperty(key);
		try {
			return Integer.parseInt(s);
		} catch (final NumberFormatException ex) {
			Configuration.log.warn("Error parsing configuration property '" + key + "', found value '" + s + "', using default value 0");
			return 0;
		}
	}
	
	public final long getLongProperty(final String key) {
		final String s = getStringProperty(key);
		try {
			return Long.parseLong(s);
		} catch (final NumberFormatException ex) {
			Configuration.log.warn("Error parsing configuration property '" + key + "', found value '" + s + "', using default value 0");
			return 0;
		}
	}
}
