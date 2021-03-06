package jaxb2.plugin.fields.init;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.tools.xjc.model.CPluginCustomization;

import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper which builds JExpression for field instantiation
 */
public class FieldInitExpressionBuilder {

    private JExpression fieldInitExpression;
    private static Map<String, JClass> refClassesMap = new HashMap<>();

    public void apply(CPluginCustomization customization, JCodeModel codeModel, ErrorHandler errorHandler, Locator fieldLocator) throws SAXException {
        if (Names.STATIC_VALUE.equals(customization.element.getLocalName())) {
            applyStaticValue(customization, codeModel);
        } else if (Names.EXECUTE_METHOD.equals(customization.element.getLocalName())) {
            applyMethodExecution(customization, codeModel, errorHandler, fieldLocator);
        }
    }

    public JExpression getFieldInitExpression() {
        return this.fieldInitExpression;
    }

    private void applyStaticValue(CPluginCustomization customization, JCodeModel codeModel) {
        JClass refBigDecimal = codeModel.ref(BigDecimal.class);
        String tagText = customization.element.getTextContent();
        fieldInitExpression = refBigDecimal.staticRef(tagText);
    }

    private void applyMethodExecution(CPluginCustomization customization, JCodeModel codeModel, ErrorHandler errorHandler, Locator fieldLocator) throws SAXException {
        if (null == fieldInitExpression) {
            fatal("Tag <" + Names.EXECUTE_METHOD + "> can't be first in the list of customizations. Hint: use <" + Names.STATIC_VALUE + "> tag as first one.", errorHandler, fieldLocator);
        }

        //Process method name
        if (customization.element.getElementsByTagNameNS(Names.SCHEMA_NAME, Names.EXECUTE_METHOD_NAME).getLength() != 1) {
            fatal("Tag <" + Names.EXECUTE_METHOD_NAME + "> must contain exactly one inner tag <" + Names.EXECUTE_METHOD_NAME + ">...</" + Names.EXECUTE_METHOD_NAME + ">", errorHandler, fieldLocator);
        }
        String methodName = customization.element.getElementsByTagNameNS(Names.SCHEMA_NAME, Names.EXECUTE_METHOD_NAME).item(0).getTextContent();
        JInvocation jInvocation = fieldInitExpression.invoke(methodName);

        //Process method params
        for (int i = 0; i < customization.element.getElementsByTagNameNS(Names.SCHEMA_NAME, Names.EXECUTE_METHOD_PARAM).getLength(); i++) {
            Node paramTag = customization.element.getElementsByTagNameNS(Names.SCHEMA_NAME, Names.EXECUTE_METHOD_PARAM).item(i);
            Node typeAttribute = paramTag.getAttributes().getNamedItem(Names.EXECUTE_METHOD_PARAM_TYPE_ATTRIBUTE);
            Node classAttribute = paramTag.getAttributes().getNamedItem(Names.EXECUTE_METHOD_PARAM_CLASS_ATTRIBUTE);
            if (null != typeAttribute) {
                String paramType = typeAttribute.getTextContent();
                String paramValue = paramTag.getTextContent();
                if (ParamType.STATIC.equals(paramType)) {
                    if (null != classAttribute) {
                        JClass refClass = refClassesMap.computeIfAbsent(classAttribute.getTextContent(), k -> codeModel.ref(classAttribute.getTextContent()));
                        jInvocation = jInvocation.arg(refClass.staticRef(paramValue));
                    } else {
                        fatal("Tag <" + Names.EXECUTE_METHOD_PARAM + " " + Names.EXECUTE_METHOD_PARAM_TYPE_ATTRIBUTE + "=\"" + ParamType.STATIC.name() + "\"" + "> must contain attribute **" + Names.EXECUTE_METHOD_PARAM_CLASS_ATTRIBUTE + "**", errorHandler, fieldLocator);
                    }
                } else if (ParamType.INT.equals(paramType)) {
                    jInvocation = jInvocation.arg(JExpr.lit(Integer.parseInt(paramValue)));
                } else {
                    fatal("Attribute **" + Names.EXECUTE_METHOD_PARAM_TYPE_ATTRIBUTE + "** of tag <" + Names.EXECUTE_METHOD_PARAM + "> contains unsupported value: " + paramType, errorHandler, fieldLocator);
                }
            } else {
                fatal("Tag <" + Names.EXECUTE_METHOD_PARAM + "> must contain attribute **" + Names.EXECUTE_METHOD_PARAM_TYPE_ATTRIBUTE + "**", errorHandler, fieldLocator);
            }
        }
        fieldInitExpression = jInvocation;
    }

    private void fatal(String msg, ErrorHandler errorHandler, Locator locator) throws SAXException {
        try {
            errorHandler.fatalError(new SAXParseException(msg, locator, null));
        } catch (SAXException ex) {
            //If exception was thrown - rethrow it. It's exactly what we want.
            throw ex;
        }
        //if method errorHandler.fatalError(..) didn't throw exception, - Do It!
        throw new SAXParseException(msg, locator, null);
    }
}
