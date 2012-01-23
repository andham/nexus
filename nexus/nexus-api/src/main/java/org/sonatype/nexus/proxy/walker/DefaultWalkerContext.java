/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.proxy.walker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.scheduling.TaskInterruptedException;
import org.sonatype.scheduling.TaskUtil;

public class DefaultWalkerContext
    implements WalkerContext
{
    private final Repository resourceStore;

    private final WalkerFilter walkerFilter;

    private final boolean collectionsOnly;

    private final ResourceStoreRequest request;

    private Map<String, Object> context;

    private List<WalkerProcessor> processors;

    private Throwable stopCause;

    private volatile boolean running;

    public DefaultWalkerContext( Repository store, ResourceStoreRequest request )
    {
        this( store, request, null );
    }

    public DefaultWalkerContext( Repository store, ResourceStoreRequest request, WalkerFilter filter )
    {
        this( store, request, filter, true, false );
    }

    public DefaultWalkerContext( Repository store, ResourceStoreRequest request, WalkerFilter filter,
                                 boolean localOnly, boolean collectionsOnly )
    {
        super();

        this.resourceStore = store;

        this.request = request;

        this.walkerFilter = filter;

        this.collectionsOnly = collectionsOnly;

        this.running = true;
    }

    public boolean isLocalOnly()
    {
        return request.isRequestLocalOnly();
    }

    public boolean isCollectionsOnly()
    {
        return collectionsOnly;
    }

    public Map<String, Object> getContext()
    {
        if ( context == null )
        {
            context = new HashMap<String, Object>();
        }
        return context;
    }

    public List<WalkerProcessor> getProcessors()
    {
        if ( processors == null )
        {
            processors = new ArrayList<WalkerProcessor>();
        }

        return processors;
    }

    public void setProcessors( List<WalkerProcessor> processors )
    {
        this.processors = processors;
    }

    public WalkerFilter getFilter()
    {
        return walkerFilter;
    }

    public Repository getRepository()
    {
        return resourceStore;
    }

    public ResourceStoreRequest getResourceStoreRequest()
    {
        return request;
    }

    public boolean isStopped()
    {
        try
        {
            TaskUtil.checkInterruption();
        }
        catch ( TaskInterruptedException e )
        {
            if ( stopCause == null )
            {
                stopCause = e;
            }

            running = false;
        }

        return !running;
    }

    public Throwable getStopCause()
    {
        return stopCause;
    }

    public void stop( Throwable cause )
    {
        running = false;

        stopCause = cause;
    }

}
