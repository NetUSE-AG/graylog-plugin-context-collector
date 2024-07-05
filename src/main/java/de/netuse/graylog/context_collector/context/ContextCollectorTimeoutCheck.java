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
import com.google.common.eventbus.Subscribe;
import com.mongodb.DBCollection;
import de.netuse.graylog.context_collector.config.PluginConfigurationV2;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.utilities.AutoValueUtils;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.antkorwin.xsync.XSync;

import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

/*
@desc This periodical checks the database for CollectionItems that timed out and puts them into the queue for the Input.
 */
public class ContextCollectorTimeoutCheck extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ContextCollectorTimeoutCheck.class);
    private JacksonDBCollection<CollectionItem, String> set_collection;
    private ArrayBlockingQueue<CollectionItem> queue;
    private XSync<String> xSync;
    private List<CollectionConfiguration> collection_configurations;
    private final ClusterConfigService clusterConfigService;

    @Inject
    ContextCollectorTimeoutCheck(final MongoConnection mongoConnection,
                                 final MongoJackObjectMapperProvider mapperProvider,
                                 final ContextCollectorCommunicationService queueProvider,
                                 final ClusterConfigService clusterConfigService) {
        final DBCollection dbCollection = mongoConnection.getDatabase().getCollection(ContextCollectorProcessor.collectionName);
        this.set_collection = JacksonDBCollection.wrap(dbCollection, CollectionItem.class, String.class, mapperProvider.get());
        queue = queueProvider.getQueue();
        xSync = queueProvider.getSync();
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public void doRun() {
        try {
            ObjectMapper mapper = new ObjectMapperProvider().get();
            PluginConfigurationV2 pluginConfiguration = this.clusterConfigService.getOrDefault(de.netuse.graylog.context_collector.config.PluginConfigurationV2.class, de.netuse.graylog.context_collector.config.PluginConfigurationV2.createDefault());
            if (pluginConfiguration.loadConfigurationFromFile()) {
                this.collection_configurations = Arrays.asList(mapper.readValue(Paths.get(pluginConfiguration.collectionConfigurationsPath()).toFile(), CollectionConfiguration[].class))
                        .stream().filter(c -> c.enabled()).collect(Collectors.toList());;
            } else {
                this.collection_configurations = pluginConfiguration.collectionConfigurations()
                                                .stream().filter(c -> c.enabled()).collect(Collectors.toList());;
            }
        } catch (IOException e) {
            LOG.error("Unable to load configuration, aborting run", e.getMessage(), e);
            return;
        }

        long now = Instant.now().getEpochSecond();
        for (CollectionItem item : set_collection.find(DBQuery.lessThan("invalid_after", now))) {
            if (queue.remainingCapacity() == 0) {
                LOG.warn("Queue is full, prematurely breaking TimeoutCheck run");
                break;
            }

            xSync.execute(item.id(), () -> {
                CollectionConfiguration configuration = getConfiguration(item);
                if (configuration == null){
                    LOG.warn("Did not find configuration for "+item.name());
                    set_collection.remove(DBQuery.is("_id", item.id()));
                    return;
                }

                CollectionItem updated = item
                                            .toBuilder()
                                            .complete(configuration.hasAllFields(item))
                                            .collection_end(now)
                                            .build();

                set_collection.remove(DBQuery.is("_id", item.id()));

                if (!item.complete() && configuration.drop_incomplete()) {
                    return;
                }

                try {
                    queue.put(updated);
                } catch (InterruptedException e) {
                    LOG.error("Unable to put CollectionItem into queue: ", e.getMessage(), e);
                }

            });
        }
    }

    private CollectionConfiguration getConfiguration(CollectionItem item) {
        for (CollectionConfiguration configuration : this.collection_configurations) {
            if (item.name().equals(configuration.name())) {
                return configuration;
            }
        }
        return null;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
    @Override
    public boolean runsForever() {
        return false;
    }
    @Override
    public boolean stopOnGracefulShutdown(){
        return false;
    }
    @Override
    public  boolean masterOnly() {
        return true;
    }
    @Override
    public  boolean startOnThisNode() {
        return true;
    }

    @Override
    public  boolean isDaemon() {
        return false;
    };

    @Override
    public int getInitialDelaySeconds() {
        return 30;
    }

    @Override
    public int getPeriodSeconds() {
        return 10;
    }

}
