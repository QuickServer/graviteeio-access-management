/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.plugins.reporter.plugin;

import io.gravitee.am.plugins.reporter.core.ReporterDefinition;
import io.gravitee.am.plugins.reporter.core.ReporterPluginManager;
import io.gravitee.am.reporter.api.Reporter;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import io.gravitee.plugin.core.api.PluginHandler;
import io.gravitee.plugin.core.api.PluginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReporterPluginHandler implements PluginHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(ReporterPluginHandler.class);

    @Autowired
    private PluginClassLoaderFactory pluginClassLoaderFactory;

    @Autowired
    private ReporterPluginManager reporterPluginManager;

    @Override
    public boolean canHandle(Plugin plugin) {
        return plugin.type() == PluginType.REPORTER;
    }

    @Override
    public void handle(Plugin plugin) {
        try {
            // disable default node reporter plugin handler
            System.setProperty("reporters." + plugin.id() + ".enabled", "false");

            ClassLoader classloader = pluginClassLoaderFactory.getOrCreateClassLoader(plugin, this.getClass().getClassLoader());

            final Class<?> reporterClass = classloader.loadClass(plugin.clazz());
            LOGGER.info("Register a new reporter plugin: {} [{}]", plugin.id(), plugin.clazz());

            Assert.isAssignable(Reporter.class, reporterClass);

            Reporter reporter = createInstance((Class<Reporter>) reporterClass);

            reporterPluginManager.register(new ReporterDefinition(reporter, plugin));
        } catch (Exception iae) {
            LOGGER.error("Unexpected error while create reporter instance", iae);
        }

    }

    private <T> T createInstance(Class<T> clazz) throws Exception {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            LOGGER.error("Unable to instantiate class: {}", ex);
            throw ex;
        }
    }
}
