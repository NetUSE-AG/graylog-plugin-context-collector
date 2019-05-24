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

import java.util.List;


/*
@desc CollectionConfiguration describes the definition for a one context collection effort.
One context collector can have several collection configurations.
 */
@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class CollectionConfiguration {
    @JsonProperty
    public abstract String type();

    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract int timeout();

    @JsonProperty
    public abstract List<String> key_fields();

    @JsonProperty
    public abstract List<String> value_fields();

    @JsonProperty
    public abstract boolean drop_incomplete();

    @JsonProperty
    public abstract boolean enabled();

    @JsonCreator
    public static CollectionConfiguration create(@JsonProperty("type") String type,
                                                     @JsonProperty("name") String name,
                                                     @JsonProperty("timeout") int timeout,
                                                     @JsonProperty("key_fields") List<String> key_fields,
                                                     @JsonProperty("value_fields") List<String> value_fields,
                                                     @JsonProperty("drop_incomplete") boolean drop_incomplete,
                                                     @JsonProperty("enabled") boolean enabled) {
        return builder()
                .type(type)
                .name(name)
                .timeout(timeout)
                .key_fields(key_fields)
                .value_fields(value_fields)
                .drop_incomplete(drop_incomplete)
                .enabled(enabled)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_CollectionConfiguration.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder type(String id);
        public abstract Builder name(String name);
        public abstract Builder timeout(int timeout);
        public abstract Builder key_fields(List<String> key_fields);
        public abstract Builder value_fields(List<String> value_fields);
        public abstract Builder drop_incomplete(boolean drop_incomplete);
        public abstract Builder enabled(boolean enabled);

        public abstract CollectionConfiguration build();
    }

    public boolean hasAllFields(CollectionItem item) {
        for (String field_name : this.value_fields()) {
            if (!item.collected_fields().containsKey(field_name)){
                return false;
            }
        }
        return true;
    }
}
