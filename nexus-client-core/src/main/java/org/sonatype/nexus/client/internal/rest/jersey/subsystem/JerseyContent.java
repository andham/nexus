/*
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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.ws.rs.core.Response;

import org.codehaus.plexus.util.IOUtil;
import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.client.rest.jersey.ContextAwareUniformInterfaceException;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * @since 2.1
 */
public class JerseyContent
    extends SubsystemSupport<JerseyNexusClient>
    implements Content
{

    private static final String CONTENT_PREFIX = "content/";

    public JerseyContent( final JerseyNexusClient nexusClient )
    {
        super( nexusClient );
    }

    @Override
    public void download( final Location location, final File target )
        throws IOException
    {
        if ( !target.exists() )
        {
            final File targetDir = target.getParentFile();
            checkState(
                ( targetDir.exists() || targetDir.mkdirs() ) && targetDir.isDirectory(),
                "Directory '%s' does not exist and could not be created", targetDir.getAbsolutePath()
            );
        }
        else
        {
            checkState(
                target.isFile() && target.canWrite(),
                "File '%s' is not a file or could not be written", target.getAbsolutePath()
            );
        }

        try
        {
            final ClientResponse response = getNexusClient()
                .uri( CONTENT_PREFIX + location.toContentPath() )
                .get( ClientResponse.class );

            if(!ClientResponse.Status.OK.equals( response.getClientResponseStatus() ))
            {
                throw getNexusClient().convert(
                    new ContextAwareUniformInterfaceException( response )
                    {
                        @Override
                        public String getMessage( final int status )
                        {
                            if ( status == Response.Status.NOT_FOUND.getStatusCode() )
                            {
                                return String.format( "Inexistent path: %s", location );
                            }
                            return null;
                        }
                    }
                );
            }

            FileOutputStream fos = null;
            try
            {
                fos = new FileOutputStream( target );
                IOUtil.copy( response.getEntityInputStream(), fos );
            }
            finally
            {
                response.close();
                IOUtil.close( fos );
            }
        }
        catch ( ClientHandlerException e )
        {
            throw getNexusClient().convert( e );
        }
    }

    @Override
    public void upload( final Location location, final File target )
        throws IOException
    {
        try
        {
            getNexusClient()
                .uri( CONTENT_PREFIX + location.toContentPath() )
                .put( target );
        }
        catch ( UniformInterfaceException e )
        {
            throw getNexusClient().convert(
                new ContextAwareUniformInterfaceException( e.getResponse() )
                {
                    @Override
                    public String getMessage( final int status )
                    {
                        if ( status == Response.Status.NOT_FOUND.getStatusCode() )
                        {
                            return String.format( "Inexistent path: %s", location );
                        }
                        return null;
                    }
                }
            );
        }
        catch ( ClientHandlerException e )
        {
            throw getNexusClient().convert( e );
        }
    }

    @Override
    public void delete( final Location location )
        throws IOException
    {
        try
        {
            getNexusClient()
                .uri( CONTENT_PREFIX + location.toContentPath() )
                .delete();
        }
        catch ( UniformInterfaceException e )
        {
            throw getNexusClient().convert(
                new ContextAwareUniformInterfaceException( e.getResponse() )
                {
                    @Override
                    public String getMessage( final int status )
                    {
                        if ( status == Response.Status.NOT_FOUND.getStatusCode() )
                        {
                            return String.format( "Inexistent path: %s", location );
                        }
                        return null;
                    }
                }
            );
        }
        catch ( ClientHandlerException e )
        {
            throw getNexusClient().convert( e );
        }
    }

}
