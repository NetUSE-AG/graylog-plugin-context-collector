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

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

import jakarta.inject.Inject;


/*
@desc This defined the Input, that creates messages from the CollectionItem passed by the queue for finished collection items.
 */
public class ContextCollectorInput extends MessageInput {

    private static final String NAME = "Context Collector Messages Input";

    @AssistedInject
    public ContextCollectorInput(@Assisted Configuration configuration,
                                ContextCollectorTransport.Factory transportFactory,
                                ContextCollectorCodec.Factory codecFactory,
                                MetricRegistry metricRegistry, LocalMetricRegistry localRegistry, ContextCollectorInput.Config config, ContextCollectorInput.Descriptor descriptor, ServerStatus serverStatus) {
        super(metricRegistry,
                configuration,
                transportFactory.create(configuration),
                localRegistry, codecFactory.create(configuration),
                config, descriptor, serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<ContextCollectorInput> {
        @Override
        ContextCollectorInput create(Configuration configuration);

        @Override
        ContextCollectorInput.Config getConfig();

        @Override
        ContextCollectorInput.Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        @Inject
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    public static class Config extends MessageInput.Config {
        @Inject
        public Config(ContextCollectorTransport.Factory transport, ContextCollectorCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
