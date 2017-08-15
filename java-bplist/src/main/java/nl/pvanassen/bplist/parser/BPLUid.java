package nl.pvanassen.bplist.parser;

/** Holder for a binary PList Uid element. */
class BPLUid implements BPListElement<Integer> {
    private final int number;

    BPLUid(int number) {
        super();
        this.number = number;
    }
    @Override
    public BPListType getType() {
        return BPListType.UID;
    }
    
    @Override
    public Integer getValue() {
        return number;
    }
}