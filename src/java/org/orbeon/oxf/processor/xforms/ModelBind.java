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
package org.orbeon.oxf.processor.xforms;

import org.jaxen.NamespaceContext;
import org.orbeon.oxf.xml.dom4j.LocationData;
import org.dom4j.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

public class ModelBind {

    private String id;
    private String nodeset;
    private String relevant;
    private String calculate;
    private String type;
    private String constraint;
    private String required;
    private String readonly;
    private NamespaceContext namespaceContext;
    private Map namespaceMap;
    private LocationData locationData;
    private ModelBind parent;
    private List children = new ArrayList();
    private Node currentNode;

    public ModelBind(String id, String nodeset, String relevant, String calculate, String type, String constraint,
                     String required, String readonly,
                     Map namespaceMap, LocationData locationData, ModelBind parent) {
        this(id, nodeset, relevant, calculate, type, constraint, required, readonly, namespaceMap, locationData);
        this.parent = parent;
    }

    public ModelBind(String id, String nodeset, String relevant, String calculate, String type, String constraint,
                     String required, String readonly,
                     Map namespaceMap, LocationData locationData) {
        this.id = id;
        this.nodeset = nodeset;
        this.relevant = relevant;
        this.calculate = calculate;
        this.type = type;
        this.constraint = constraint;
        this.required = required;
        this.readonly = readonly;
        this.namespaceMap = namespaceMap;
        this.locationData = locationData;
    }

    public String getId() {
        return id;
    }

    public String getNodeset() {
        return nodeset;
    }

    public String getRelevant() {
        return relevant;
    }

    public String getCalculate() {
        return calculate;
    }

    public String getType() {
        return type;
    }

    public String getConstraint() {
        return constraint;
    }

    public String getRequired() {
        return required;
    }

    public String getReadonly() {
        return readonly;
    }

    public Map getNamespaceMap() {
        return namespaceMap;
    }

    public LocationData getLocationData() {
        return locationData;
    }

    public void addChild(ModelBind bind) {
        children.add(bind);
    }

    public Iterator getChildrenIterator() {
        return children.iterator();
    }

    public Node getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(Node currentNode) {
        this.currentNode = currentNode;
    }

    public ModelBind getParent() {
        return parent;
    }

    public void setParent(ModelBind parent) {
        this.parent = parent;
    }

    public String toString() {
        return "ModelBind: id="+getId() + " nodeset="+getNodeset();
    }
}
