package jaxb2.plugin.fields.init;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;

import org.jvnet.jaxb2_commons.plugin.AbstractParameterizablePlugin;
import org.jvnet.jaxb2_commons.util.CustomizationUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

public class InitBigDecimalFieldsPlugin extends AbstractParameterizablePlugin {

    private static final String OPTION_NAME = "Xinit-bigdecimal-fields";

    private static final String NAMESPACE_URI = "http://init.bigdecimal";

    @Override
    public String getOptionName() {
        return OPTION_NAME;
    }

    @Override
    public String getUsage() {
        return "-" + OPTION_NAME + ": Instantiate BigDecimal fields";
    }

    /**
     * Plugin entry-point
     */
    @Override
    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {

        for (ClassOutline classOutline : outline.getClasses()) {
            for (FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
                //Skip Non-BigDecimal.class fields
                if (!BigDecimal.class.getCanonicalName().equals(fieldOutline.getRawType().fullName())) {
                    continue;
                }

                initBigDecimalFields(errorHandler, classOutline, fieldOutline);
            }
        }

        return false;
    }

    /**
     * Builds JExpression and uses it to instantiate given field
     */
    private void initBigDecimalFields(ErrorHandler errorHandler, ClassOutline classOutline, FieldOutline fieldOutline) throws SAXException {
        Map<String, JFieldVar> fields = classOutline.implClass.fields();
        Locator fieldLocator = fieldOutline.getPropertyInfo().getLocator();

        List<CPluginCustomization> bdCustomizations = getBigDecimalNameSpaceCustomizations(fieldOutline);

        JExpression fieldInitExpression = buildFieldInitExpression(bdCustomizations, classOutline, errorHandler, fieldLocator);

        //Apply initializations to field, if any were found
        if (null != fieldInitExpression) {
            JFieldVar field = fields.get(fieldOutline.getPropertyInfo().getName(false));
            field.init(fieldInitExpression);
        }
    }

    /**
     * Processes given customizations and builds JExpression for field instantiation
     */
    private JExpression buildFieldInitExpression(List<CPluginCustomization> bdCustomizations, ClassOutline classOutline, ErrorHandler errorHandler, Locator fieldLocator) throws SAXException {
        JCodeModel codeModel = classOutline.implClass.owner();

        FieldInitExpressionBuilder fieldInitExpressionBuilder = new FieldInitExpressionBuilder();
        for (CPluginCustomization customization : bdCustomizations) {
            //Mark that this customization was processed
            customization.markAsAcknowledged();

            fieldInitExpressionBuilder.apply(customization, codeModel, errorHandler, fieldLocator);
        }
        return fieldInitExpressionBuilder.getFieldInitExpression();
    }

    /**
     * Return all customizations from BigDecimal namespace
     */
    private List<CPluginCustomization> getBigDecimalNameSpaceCustomizations(FieldOutline fieldOutline) {
        return CustomizationUtils.getCustomizations(fieldOutline).stream()
                .filter(cPluginCustomization ->
                        NAMESPACE_URI.equals(cPluginCustomization.element.getNamespaceURI()))
                .collect(Collectors.toList());
    }

    /**
     * Returns list of QNAMEs which we know how to handle
    **/
    @Override
    public Collection<QName> getCustomizationElementNames() {
        return Arrays.asList(
                new QName(NAMESPACE_URI, Names.STATIC_VALUE),
                new QName(NAMESPACE_URI, Names.EXECUTE_METHOD),
                new QName(NAMESPACE_URI, Names.EXECUTE_METHOD_NAME),
                new QName(NAMESPACE_URI, Names.EXECUTE_METHOD_PARAM)
        );
    }
}
