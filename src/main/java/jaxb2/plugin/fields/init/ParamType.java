package jaxb2.plugin.fields.init;

/**
 * Enum that contains param-types which we will process
 */
public enum ParamType {
    BIG_DECIMAL("BigDecimal"), INT("int");

    private String name;

    ParamType(String name) {
        this.name = name;
    }

    public boolean equals(String name) {
            return this.name.equalsIgnoreCase(name);
        }
}
