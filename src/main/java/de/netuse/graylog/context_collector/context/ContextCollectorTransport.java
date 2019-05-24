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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.GeneratorTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;


public class ContextCollectorTransport extends GeneratorTransport {
    private static final Logger LOG = LoggerFactory.getLogger(ContextCollectorTransport.class);

    private final ObjectMapper objectMapper;
    private ArrayBlockingQueue<CollectionItem> queue;

    @AssistedInject
    public ContextCollectorTransport(@Assisted Configuration configuration, EventBus eventBus, ObjectMapper objectMapper, ContextCollectorCommunicationService queueProvider) {
        super(eventBus, configuration);
        this.objectMapper = objectMapper;
        queue = queueProvider.getQueue();
    }

    @Override
    protected RawMessage produceRawMessage(MessageInput input) {
        try {
            CollectionItem item = queue.take();
            final byte[] payload = objectMapper.writeValueAsBytes(item);
            return new RawMessage(payload);
        } catch (InterruptedException e) {
            LOG.error("Unable to put CollectionItem into queue: ", e.getMessage(), e);
        } catch ( JsonProcessingException e) {
            LOG.error("Unable to serialize CollectionItem ", e.getMessage(), e);
        }
        return null;
    }


    @FactoryClass
    public interface Factory extends Transport.Factory<ContextCollectorTransport> {
        @Override
        ContextCollectorTransport create(Configuration configuration);

        @Override
        ContextCollectorTransport.Config getConfig();
    }

    @ConfigClass
    public static class Config extends GeneratorTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest c = super.getRequestedConfiguration();
            return c;
        }
    }
}
