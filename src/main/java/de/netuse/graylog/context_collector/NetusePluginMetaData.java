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

package de.netuse.graylog.context_collector;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class NetusePluginMetaData implements PluginMetaData {
    private static final String PLUGIN_PROPERTIES = "de.netuse.graylog.context_collector/graylog-plugin.properties";

    @Override
    public String getUniqueId() {
        return "de.netuse.graylog.context_collector.NetusePlugin";
    }

    @Override
    public String getName() {
        return "NetUSE Context CollectorPlugin";
    }

    @Override
    public String getAuthor() {
        return "Janosch Rux <jru@netuse.de> NetUSE AG";
    }

    @Override
    public URI getURL() {
        return URI.create("https://netuse.de");
    }

    @Override
    public Version getVersion() {
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "version", Version.from(2, 2, 4, "unknown"));
    }

    @Override
    public String getDescription() {
        return "Provides the NetUSE AG Context Collector";
    }

    @Override
    public Version getRequiredVersion() {
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "graylog.version", Version.from(4, 3, 0, "unknown"));
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
