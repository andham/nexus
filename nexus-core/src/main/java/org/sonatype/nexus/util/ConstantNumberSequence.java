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
package org.sonatype.nexus.util;

/**
 * A simple sequence that is a constant number sequence.
 * 
 * @author cstamas
 */
public class ConstantNumberSequence
    implements NumberSequence
{
    private final long val;

    public ConstantNumberSequence( long val )
    {
        this.val = val;
    }

    public long next()
    {
        return peek();
    }

    public long prev()
    {
        return peek();
    }

    public long peek()
    {
        return val;
    }

    public void reset()
    {
        // nothing
    }
}
