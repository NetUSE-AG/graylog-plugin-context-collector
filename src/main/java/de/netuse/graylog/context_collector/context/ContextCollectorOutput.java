/*
 * Copyright (C) NetUSE AG - All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */

package de.netuse.graylog.context_collector.context;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import de.netuse.graylog.context_collector.config.PluginConfigurationV2;
import org.graylog2.bindings.providers.ClusterEventBusProvider;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.utilities.AutoValueUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/*
This defines the Output that takes Messages that are to be process by the Context Collector.
 */
public class ContextCollectorOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(ContextCollectorOutput.class);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicReference<ContextCollectorProcessor> processor = new AtomicReference<>();

    private final ClusterConfigService clusterConfigService;
    private final MongoConnection mongoConnection;
    private final MongoJackObjectMapperProvider mapperProvider;
    private final ContextCollectorCommunicationService queueProvider;

    @Inject
    public ContextCollectorOutput(
            final MongoConnection mongoConnection,
            final MongoJackObjectMapperProvider mapperProvider,
            final ClusterConfigService clusterConfigService,
            final ContextCollectorCommunicationService queueProvider,
            final ClusterEventBusProvider clusterEventBusProvider,
            final EventBus serverEventBus) throws MessageOutputConfigurationException, IOException {
        LOG.info("Initializing");
        isRunning.set(true);

        this.clusterConfigService = clusterConfigService;
        this.mongoConnection = mongoConnection;
        this.mapperProvider = mapperProvider;
        this.queueProvider = queueProvider;
        serverEventBus.register(this);

        final PluginConfigurationV2 configuration = clusterConfigService.getOrDefault(PluginConfigurationV2.class, PluginConfigurationV2.createDefault());
        processor.set(new ContextCollectorProcessor(mongoConnection, mapperProvider, queueProvider, configuration));

    }

    @Subscribe
    public void handleUpdatedClusterConfig(ClusterConfigChangedEvent clusterConfigChangedEvent) throws Exception{
        if (!clusterConfigChangedEvent.type().equals(AutoValueUtils.getCanonicalName((PluginConfigurationV2.class)))) {
            return;
        }
        LOG.info("Config changed, reload processor");
        final PluginConfigurationV2 configuration = clusterConfigService.getOrDefault(PluginConfigurationV2.class, PluginConfigurationV2.createDefault());
        processor.set(new ContextCollectorProcessor(mongoConnection, mapperProvider, queueProvider, configuration));
    }


    @Override
    public void stop() {
        isRunning.set(false);
        LOG.info("Stopping");
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        //Mumsie, what's a feedback loop?
        if (message.hasField("log.logger") && message.getField("log.logger").toString().equals("ContextCollector")) {
            return;
        }
        processor.get().processMessage(message);

    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message message : messages) {
            write(message);
        }

    }

    @FactoryClass
    public interface Factory extends MessageOutput.Factory<ContextCollectorOutput> {
        @Override
        ContextCollectorOutput create(Stream stream, Configuration configuration);

        @Override
        ContextCollectorOutput.Config getConfig();

        @Override
        ContextCollectorOutput.Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Context Collector Output", false, "", "Output that routes messages to the Context Collector");
        }
    }


    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            ConfigurationRequest configurationRequest = new ConfigurationRequest();
            return configurationRequest;
        }
    }
}
