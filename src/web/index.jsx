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

//import 'webpack-entry';
// eslint-disable-next-line no-unused-vars
//import webpackEntry from 'webpack-entry';
import './webpack-entry';

import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import packageJson from '../../package.json';

import NetuseContextCollectorConfig from './NetuseContextCollectorConfig';

const manifest = new PluginManifest(packageJson, {

      systemConfigurations: [
        {
          component: NetuseContextCollectorConfig,
          configType: 'de.netuse.graylog.context_collector.config.PluginConfigurationV2',
          displayName: 'Context Collector',
        },
      ]
});

PluginStore.register(manifest);
