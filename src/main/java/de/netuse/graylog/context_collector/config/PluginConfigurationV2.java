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

package de.netuse.graylog.context_collector.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import de.netuse.graylog.context_collector.context.CollectionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
@AutoValue
public abstract class PluginConfigurationV2 {
    private static final Logger LOG = LoggerFactory.getLogger(PluginConfigurationV2.class);

    @JsonProperty("load_configuration_from_file")
    public abstract boolean loadConfigurationFromFile();

    @JsonProperty("collection_configurations_path")
    public abstract String collectionConfigurationsPath();

    @JsonProperty("collection_configurations")
    public abstract List<CollectionConfiguration> collectionConfigurations();

    @JsonCreator
    public static PluginConfigurationV2 create(@JsonProperty("collection_configurations") List<CollectionConfiguration> collectionConfigrations,
                                               @JsonProperty("collection_configurations_path") String collectionConfigrationsPath,
                                               @JsonProperty("load_configuration_from_file") boolean loadConfigurationFromFile) {
        return builder()
                .collectionConfigurations(collectionConfigrations)
                .collectionConfigurationsPath(collectionConfigrationsPath)
                .loadConfigurationFromFile(loadConfigurationFromFile)
                .build();
    }

    public static PluginConfigurationV2 createDefault() {
        return builder()
                .collectionConfigurations(new ArrayList<CollectionConfiguration>())
                .collectionConfigurationsPath("/etc/graylog/context_collector.json")
                .loadConfigurationFromFile(false)
                .build();
    }

    static Builder builder() {
        return new AutoValue_PluginConfigurationV2.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder loadConfigurationFromFile(boolean loadConfigurationFromFile);
        public abstract Builder collectionConfigurations(List<CollectionConfiguration> collectionConfigurations);
        public abstract Builder collectionConfigurationsPath(String collectionConfigurationsPath);

        public abstract PluginConfigurationV2 build();
    }

}
