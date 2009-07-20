package org.orbeon.saxon.dom4j;

import org.dom4j.Node;
import org.orbeon.oxf.xforms.InstanceData;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.SingletonIterator;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.UntypedAtomicValue;
import org.orbeon.saxon.value.Value;

/**
 * This wrapper is an extension of the Saxon node wrapper which is aware of XForms type annotations.
 */
public class TypedNodeWrapper extends NodeWrapper {

    protected TypedNodeWrapper(Object node, NodeWrapper parent, int index) {
        super(node, parent, index);
    }

    @Override
    protected NodeWrapper makeWrapper(Object node, DocumentWrapper docWrapper, NodeWrapper parent, int index) {
        return makeTypedWrapper(node, docWrapper, parent, index);
    }

    static NodeWrapper makeTypedWrapper(Object node, DocumentWrapper docWrapper, NodeWrapper parent, int index) {
        NodeWrapper wrapper;
        final Node dom4jNode = (Node) node;
        switch (dom4jNode.getNodeType()) {
            case Type.DOCUMENT:
                return docWrapper;
            case Type.ELEMENT:
            case Type.ATTRIBUTE:
            case Type.COMMENT:
            case Type.PROCESSING_INSTRUCTION:
            case Type.NAMESPACE:
            case Type.TEXT:
                wrapper = new TypedNodeWrapper(node, parent, index);
                wrapper.nodeKind = dom4jNode.getNodeType();
                break;
            case 4: // dom4j CDATA
                wrapper = new TypedNodeWrapper(node, parent, index);
                wrapper.nodeKind = Type.TEXT;
                break;
            default:
                throw new IllegalArgumentException("Bad node type in dom4j: " + node.getClass() + " instance " + node.toString());
        }

        wrapper.docWrapper = docWrapper;
        return wrapper;
    }

    @Override
    public SequenceIterator getTypedValue() throws XPathException {

        int annotation = getTypeAnnotation();
        if ((annotation & NodeInfo.IS_DTD_TYPE) != 0) {
            annotation = StandardNames.XDT_UNTYPED_ATOMIC;
        }
        annotation &= NamePool.FP_MASK;
        if (annotation == -1 || annotation == StandardNames.XDT_UNTYPED_ATOMIC || annotation == StandardNames.XDT_UNTYPED) {
            return SingletonIterator.makeIterator(new UntypedAtomicValue(getStringValueCS()));
        } else {
            SchemaType stype = getConfiguration().getSchemaType(annotation);
            if (stype == null) {
                String typeName;
                try {
                    typeName = getNamePool().getDisplayName(annotation);
                } catch (Exception err) {
                    typeName = annotation + "";
                }
                throw new DynamicError("Unknown type annotation " +
                        Err.wrap(typeName) + " in document instance");
            } else {
                return stype.getTypedValue(this);
            }
        }
    }

    @Override
    public Value atomize() throws XPathException {
        int annotation = getTypeAnnotation();
        if ((annotation & NodeInfo.IS_DTD_TYPE) != 0) {
            annotation = StandardNames.XDT_UNTYPED_ATOMIC;
        }
        annotation &= NamePool.FP_MASK;
        if (annotation == -1 || annotation == StandardNames.XDT_UNTYPED_ATOMIC || annotation == StandardNames.XDT_UNTYPED) {
            return new UntypedAtomicValue(getStringValueCS());
        } else {
            SchemaType stype = getConfiguration().getSchemaType(annotation);
            if (stype == null) {
                String typeName;
                try {
                    typeName = getNamePool().getDisplayName(annotation);
                } catch (Exception err) {
                    typeName = annotation + "";
                }
                throw new DynamicError("Unknown type annotation " +
                        Err.wrap(typeName) + " in document instance");
            } else {
                return stype.atomize(this);
            }
        }
    }

    @Override
    public int getTypeAnnotation() {

        final String nodeType = InstanceData.getType((Node) node);
        if (nodeType == null) {
            if (getNodeKind() == Type.ATTRIBUTE) {
                return StandardNames.XDT_UNTYPED_ATOMIC;
            }
            return StandardNames.XDT_UNTYPED;
        } else {
            // Extract QName
            final String uri;
            final String localname;
            {
                int openIndex = nodeType.indexOf("{");
                if (openIndex == -1) {
                    uri = "";
                    localname = nodeType;
                } else {
                    uri = nodeType.substring(openIndex + 1, nodeType.indexOf("}"));
                    localname = nodeType.substring(nodeType.indexOf("}") + 1);
                }
            }
            final int fingerprint = StandardNames.getFingerprint(uri, localname);
            if (fingerprint == -1) {
                // Back to default case
                if (getNodeKind() == Type.ATTRIBUTE) {
                    return StandardNames.XDT_UNTYPED_ATOMIC;
                }
                return StandardNames.XDT_UNTYPED;
            } else {
                // Return identified type
                return fingerprint;
            }
        }
    }
}
