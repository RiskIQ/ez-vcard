package ezvcard.io.json.namesilo;

import ezvcard.io.json.JsonValue;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author dpon
 * created 7/29/20
 */
public class NamesiloValue {
    private String stringValue;
    private String typeName;
    private List<NamesiloProperty> components;
    private List<NamesiloValue> values;

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public List<NamesiloProperty> getComponents() {
        return components;
    }

    public void setComponents(List<NamesiloProperty> components) {
        this.components = components;
    }

    public List<NamesiloValue> getValues() {
        return values;
    }

    public void setValues(List<NamesiloValue> values) {
        this.values = values;
    }

    public Object getValue() {
        if (stringValue != null)
            return stringValue;
        if (values != null && !values.isEmpty()) {
            return values.stream().map(namesiloValue -> new JsonValue(namesiloValue.getStringValue())).collect(Collectors.toList());
        }
        return null;
    }
}
