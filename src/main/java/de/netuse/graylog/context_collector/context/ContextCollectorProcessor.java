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

import com.antkorwin.xsync.XSync;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import de.netuse.graylog.context_collector.config.PluginConfigurationV2;
import org.bson.Document;
import org.bson.Transformer;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.Message;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

/*
@desc This processor consumes messages sent to Context Collector Outputs. It checks whether a message matches a CollectionConfiguration and if so
creates or updates a CollectionItem. If all fields are collected, the Item is dispatched to the Input queue.
Since processor run in parallel (localy and globaly), to do these operations reliably they best need to sequence all DB reads and writes concerning the same CollectionItem.
CollectionItems are identified by an ID constructed by computing SHA256(key_field_name + key_field_value).
 */
public class ContextCollectorProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ContextCollectorProcessor.class);
    List<CollectionConfiguration> collection_configurations = null;
    final static String collectionName = "contextcollector_collections";
    private  JacksonDBCollection<CollectionItem, String> set_collection;
    private ArrayBlockingQueue<CollectionItem> queue;
    private XSync<String> xSync;

    MongoJackObjectMapperProvider mapperProvider;


    ContextCollectorProcessor(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapperProvider, ContextCollectorCommunicationService queueProvider, PluginConfigurationV2 pluginConfiguration) throws IOException {
        this.mapperProvider = mapperProvider;

        final DBCollection dbCollection = mongoConnection.getDatabase().getCollection(collectionName);
        this.set_collection = JacksonDBCollection.wrap(dbCollection, CollectionItem.class, String.class, mapperProvider.get());
        this.set_collection.createIndex(new BasicDBObject("invalid_after", 1));
        ObjectMapper mapper = new ObjectMapperProvider().get();

        if (pluginConfiguration.loadConfigurationFromFile()) {
            collection_configurations = Arrays.asList(mapper.readValue(Paths.get(pluginConfiguration.collectionConfigurationsPath()).toFile(),CollectionConfiguration[].class))
                    .stream().filter(c -> c.enabled()).collect(Collectors.toList());
        } else {

            collection_configurations = pluginConfiguration.collectionConfigurations().stream().filter(c -> c.enabled()).collect(Collectors.toList());
        }

        this.queue = queueProvider.getQueue();
        this.xSync = queueProvider.getSync();
    }


    void processMessage(Message message) throws NoSuchAlgorithmException {
        for (CollectionConfiguration configuration : collection_configurations) {
            if (has_key_fields(message, configuration.key_fields())) {
                updateFromMessage(message, configuration);
            }
        }
    }

    private boolean has_key_fields(Message message, List<String> key_fields) {
        return key_fields.stream().allMatch(message::hasField);
    }

    private  void updateFromMessage(Message message, CollectionConfiguration configuration) throws NoSuchAlgorithmException {
        String id = constructId(message, configuration);

        xSync.execute(id, () -> {
            CollectionItem item;
            item = getCollectionItem(id);

            if (item == null) {
                item = createCollectionItem(id, configuration, message);
            }

            if (item == null) {
                LOG.error("Item "+id+" was null, even after creating it.");
                return;
            }

            for (String field : configuration.value_fields()) {
                if (message.hasField(field)) {
                    item.collected_fields().put(field, message.getField(field));
                }
            }

            /*
            It is possible for collections to be complete:false, while all fields exist.
            Assume three fields that are collected: field_1, field_2, field_3

            Each is sent to the resp. graylog node. Field_1 might be processed slightly earlier
            than field_2 and field_3. When graylog node 2 and 3 retrieve the CollectionItem from
            the database, might just contain field_1 as collected. Adding field_2 or field_3
            will yield hasAllFields() to be false. When both nodes send their update to the
            Database the fields will be merged, but complete remain false.

            This is why the TimeoutCheck rebuilds the complete field with hasAllFields().
            */

            boolean complete = configuration.hasAllFields(item);
            item = item.toBuilder()
                    .complete(complete)
                    .invalid_after(Instant.now().getEpochSecond() + configuration.timeout())
                    .collection_end(Instant.now().getEpochSecond())
                    .build();

            if (item.complete() && queue.remainingCapacity() > 0) {
                LOG.debug("All fields collected and queue not full, writing");
                set_collection.remove(DBQuery.is("_id", item.id()));
                try {
                    queue.put(item);
                } catch (InterruptedException e) {
                    LOG.error("Unable to put CollectionItem into queue: ", e.getMessage(), e);
                }
            } else {
                /*
                    If for some reason complete message are not consumed fast enough,
                    then they're updated in the database and will drained later by
                    the timeout check. Otherwise they would be dropped on the spot.
                    And that would be a shame.
                 */
                if(item.complete()) {
                    LOG.warn("Writing complete item back to database, Queue has no capacity");
                }
                Document updateFields = new Document();
                updateFields.append("invalid_after", item.invalid_after());
                updateFields.append("complete", item.complete());
                for ( Map.Entry<String, Object> set: item.collected_fields().entrySet()) {
                    updateFields.append("collected_fields."+set.getKey(), set.getValue());
                }
                BasicDBObject setQuery = new BasicDBObject();
                setQuery.append("$set", updateFields);
                WriteResult<CollectionItem, String> result = set_collection.update(new BasicDBObject("_id", item.id()), setQuery, false, false);
                if (!result.wasAcknowledged()) {
                    LOG.error("Update of CollectionItem was not acknowledged.");
                }
            }
        });
    }

    private boolean hasCollectionItem(String collection_id) {
        return set_collection.getCount(DBQuery.is("_id", collection_id)) > 0;
    }

    private CollectionItem createCollectionItem(String collection_id, CollectionConfiguration configuration, Message message) {
        long now = Instant.now().getEpochSecond();
        HashMap<String, Object> fields = new HashMap<String, Object>();
        for (String field : configuration.key_fields()) {
            if (message.hasField(field)) {
                fields.put(field, message.getField(field));
            }
        }

        CollectionItem item = CollectionItem.create(collection_id,
                configuration.name(),
                now + configuration.timeout(),
                fields,
                now,
                now,
                false);

        WriteResult<CollectionItem, String> result = set_collection.update(DBQuery.is("_id", collection_id), item, true, false);
        if (!result.wasAcknowledged()) {
            LOG.error("Creation of CollectionItem was not acknowledged.");
        }
        return item;
    }


    private CollectionItem getCollectionItem(String id) {
        return set_collection.findOne(DBQuery.is("_id", id));
    }

    private String constructId(Message message, CollectionConfiguration configuration) throws NoSuchAlgorithmException {
        String id = configuration.name() +
                    configuration.key_fields().stream()
                            .sorted().map(name ->  name + message.getField(name).toString())
                            .collect(Collectors.joining());
        MessageDigest digest = MessageDigest.getInstance("SHA256");
        return bytesToHex(digest.digest(id.getBytes(StandardCharsets.UTF_8)));
    }


    //Courtesy of SO 9655181
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
