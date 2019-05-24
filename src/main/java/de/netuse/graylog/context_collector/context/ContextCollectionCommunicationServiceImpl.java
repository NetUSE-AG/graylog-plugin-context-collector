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

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;


public class ContextCollectionCommunicationServiceImpl implements ContextCollectorCommunicationService{
    private static ArrayBlockingQueue<CollectionItem> queue = null;
    private static XSync<String> xSync = null;

    ContextCollectionCommunicationServiceImpl() throws IOException {
        if (queue == null) {
           queue = new ArrayBlockingQueue<>(1024);
        }

        if (xSync == null) {
            xSync = new XSync<String>();
        }
    }

    @Override
    public ArrayBlockingQueue<CollectionItem> getQueue() {
        return queue;
    }

    @Override
    public XSync<String> getSync() {
        return xSync;
    }
}
