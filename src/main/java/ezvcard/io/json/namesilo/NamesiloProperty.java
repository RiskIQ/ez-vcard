package ezvcard.io.json.namesilo;

/**
 * @author dpon
 * created 7/29/20
 */
public class NamesiloProperty {

    private String name;
    private NamesiloValue value;
    private Object parameters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NamesiloValue getValue() {
        return value;
    }

    public void setValue(NamesiloValue value) {
        this.value = value;
    }

    public Object getParameters() {
        return parameters;
    }

    public void setParameters(Object parameters) {
        this.parameters = parameters;
    }
}
