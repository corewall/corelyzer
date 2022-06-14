package corelyzer.data;

public enum CoreSectionTieType {
    NONE(0), VISUAL(1), DATA(2), SPLICE(3);
    private final int value;
    private CoreSectionTieType(int value) {
        this.value = value;
    }
    public int intValue() { return value; }
    public static CoreSectionTieType fromInt(int i) {
        switch (i) {
            case 1:
                return VISUAL;
            case 2:
                return DATA;
            case 3:
                return SPLICE;
            default:
                return NONE;
        }
    }
    public String toString() {
        switch (this.value) {
            case 1:
                return "Visual";
            case 2:
                return "Data";
            case 3:
                return "Splice";
            default:
                return "NONE";
        }
    }
}
