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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Map;

/*
@desc A CollectionItem holds all the information for the collection for a specific key value.
They're stored in the MongoDB, and once the collection finished, passed through the queue to
the ContextCollectorInput
 */
@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class CollectionItem {

    @Id
    @Nullable
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("name")
    public abstract String name();

    @JsonProperty("invalid_after")
    public abstract long invalid_after();

    @JsonProperty("collected_fields")
    public abstract Map<String,Object> collected_fields();

    @JsonProperty("collection_start")
    public abstract long collection_start();

    @JsonProperty("collection_end")
    public abstract long collection_end();

    @JsonProperty("complete")
    public abstract boolean complete();

    @JsonCreator
    public static CollectionItem create(@JsonProperty("id") String id, @JsonProperty("name") String name,
                                                 @JsonProperty("invalid_after") long invalid_after,
                                                 @JsonProperty("collected_fields") Map<String,Object> collected_fields,
                                                 @JsonProperty("collection_start") long collection_start,
                                                 @JsonProperty("collection_end") long collection_end,
                                                 @JsonProperty("complete") boolean complete) {
        return builder()
                .id(id)
                .name(name)
                .invalid_after(invalid_after)
                .collected_fields(collected_fields)
                .collection_start(collection_start)
                .collection_end(collection_end)
                .complete(complete)
                .build();
    }
    public static Builder builder() {
        return new AutoValue_CollectionItem.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);
        public abstract Builder name(String name);
        public abstract Builder invalid_after(long invalid_after);
        public abstract Builder collected_fields(Map<String,Object> collected_fields);
        public abstract Builder collection_start(long collection_start);
        public abstract Builder collection_end(long collection_end);
        public abstract Builder complete(boolean complete);
        public abstract CollectionItem build();
    }

}
