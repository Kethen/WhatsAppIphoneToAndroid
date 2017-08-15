package nl.pvanassen.bplist.parser;

class BPListLong implements BPListElement<Long> {
    private final long value;
    
    BPListLong(long value) {
        this.value = value;
    }
    @Override
    public BPListType getType() {
        return BPListType.LONG;
    }
    
    @Override
    public Long getValue() {
        return value;
    }
}
