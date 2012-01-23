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
package org.sonatype.nexus.gwt.ui.client.form;

import com.google.gwt.user.client.ui.TextBox;

/**
 * 
 *
 * @author barath
 */
public class NumberBoxInput implements FormInput {

    private TextBox widget;
    
    private boolean integer;
    
    public NumberBoxInput(TextBox widget) {
        this(widget, true);
    }

    public NumberBoxInput(TextBox widget, boolean isInteger) {
        this.widget = widget;
        this.integer = isInteger;
    }

    public Object getValue() {
        Double n = null;
        if (widget.getText() != null && !"".equals(widget.getText().trim())) {
            n = Double.valueOf(widget.getText());
        }
        return n;
    }
    
    public void setValue(Object value) {
        if (value != null) {
            String s = String.valueOf(value);
            if (integer) {
                int pos = s.indexOf('.');
                if (pos > -1) {
                    s = s.substring(0, pos);
                }
            }
            widget.setText(s);
        } else {
            widget.setText("");
        }
    }

    public void reset() {
        widget.setText("");
    }

}
