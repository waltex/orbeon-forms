/**
 *  Copyright (C) 2004 Orbeon, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version
 *  2.1 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.processor.xforms.input.action;

import org.dom4j.Document;
import org.jaxen.FunctionContext;
import org.orbeon.oxf.pipeline.api.PipelineContext;

import java.util.Map;

public interface Action {

    public final String NODESET_ATTRIBUTE_NAME = "node-ids";
    public final String AT_ATTRIBUTE_NAME = "at";
    public final String POSITION_ATTRIBUTE_NAME = "position";
    public final String VALUE_ATTRIBUTE_NAME = "value";
    public final String CONTENT_ATTRIBUTE_NAME = "content";

    public void setParameters(Map parameters);
    public void run(PipelineContext context, FunctionContext functionContext, Document instance);

}
