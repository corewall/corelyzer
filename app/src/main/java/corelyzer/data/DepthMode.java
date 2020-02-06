package corelyzer.data;

public enum DepthMode {
    SECTION_DEPTH, ACCUM_DEPTH;
    
    public String toString() {
        if (this.ordinal() == 0)
            return "Section Depth";
        else
            return "Accumulated Depth";
    }
}