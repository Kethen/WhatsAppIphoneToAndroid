package nl.pvanassen.bplist.parser;

class BPListData implements BPListElement<byte[]> {
    private final byte[] value;
    
    BPListData(byte[] value) {
        this.value = value;
    }
    @Override
    public BPListType getType() {
        return BPListType.DATA;
    }
    
    @Override
    public byte[] getValue() {
        return value;
    }
}
