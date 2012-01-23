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
package org.sonatype.nexus.error.reporting;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.security.model.CUser;
import org.sonatype.security.model.Configuration;
import org.sonatype.security.model.io.xpp3.SecurityConfigurationXpp3Writer;
import org.sonatype.security.model.source.SecurityModelConfigurationSource;

public class SecurityXmlHandler
    extends AbstractXmlHandler
{
    public File getFile( SecurityModelConfigurationSource source, NexusConfiguration nexusConfig )
        throws IOException
    {
        Configuration configuration = 
            ( Configuration ) cloneViaXml( source.getConfiguration() );
        
        // No config ?
        if ( configuration == null )
        {
            return null;
        }
        
        for ( CUser user : ( List<CUser> ) configuration.getUsers() )
        {
            user.setPassword( PASSWORD_MASK );
            user.setEmail( PASSWORD_MASK );
        }
        
        SecurityConfigurationXpp3Writer writer = new SecurityConfigurationXpp3Writer();
        FileWriter fWriter = null;
        File tempFile = null;
        
        try
        {
            tempFile = new File( nexusConfig.getTemporaryDirectory(), "security.xml." + System.currentTimeMillis() );
            fWriter = new FileWriter( tempFile );
            writer.write( fWriter, configuration );
        }
        finally
        {
            if ( fWriter != null )
            {
                fWriter.close();
            }
        }
        
        return tempFile;
    }
}
