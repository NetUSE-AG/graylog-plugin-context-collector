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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/*
@desc Part of the Input, this class transforms the CollectionItem into a new message.
 */
@Codec(name = "context-collector-message", displayName = "Context Collector Input")
public class ContextCollectorCodec extends AbstractCodec {
    private static final Logger LOG = LoggerFactory.getLogger(ContextCollectorCodec.class);
    private final ObjectMapper objectMapper;
    private final MessageFactory messageFactory;

    @Inject
    public ContextCollectorCodec(@Assisted Configuration configuration, ObjectMapper objectMapper, MessageFactory messageFactory) {
        super(configuration);
        this.objectMapper = objectMapper;
	this.messageFactory = messageFactory;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        if (!rawMessage.getCodecName().equals(getName())) {
            LOG.error("Cannot decode payload type {}, skipping message {}",
                    rawMessage.getCodecName(), rawMessage.getId());
            return null;
        }
        try {
            final CollectionItem item = objectMapper.readValue(rawMessage.getPayload(), CollectionItem.class);
            final Message message = this.messageFactory.createMessage("Collected log", "graylog", DateTime.now());
            message.addFields(item.collected_fields());
            message.addField("log.logger","ContextCollector");
            message.addField("context_collector_timeout", new DateTime(item.invalid_after()*1000));
            message.addField("context_collector_name", item.name());
            message.addField("context_collector_start", new DateTime(item.collection_start()*1000));
            message.addField("context_collector_end", new DateTime(item.collection_end()*1000));
            message.addField("context_collector_complete", item.complete());

            return message;
        } catch (IOException e) {
            LOG.error("Cannot decode message to class CollectionItem", e.getMessage(), e);
        }
        return null;
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }


    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<ContextCollectorCodec> {
        @Override
        ContextCollectorCodec create(Configuration configuration);

        @Override
        ContextCollectorCodec.Config getConfig();

        @Override
        ContextCollectorCodec.Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {

        }
    }

    public static class Descriptor extends AbstractCodec.Descriptor {
        @Inject
        public Descriptor() {
            super(ContextCollectorCodec.class.getAnnotation(Codec.class).displayName());
        }
    }
}
