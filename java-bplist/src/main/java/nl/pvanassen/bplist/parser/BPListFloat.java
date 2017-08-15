package nl.pvanassen.bplist.parser;

class BPListFloat implements BPListElement<Float> {
    private final float value;
    
    BPListFloat(float value) {
        this.value = value;
    }
    @Override
    public BPListType getType() {
        return BPListType.FLOAT;
    }
    
    @Override
    public Float getValue() {
        return value;
    }
}
