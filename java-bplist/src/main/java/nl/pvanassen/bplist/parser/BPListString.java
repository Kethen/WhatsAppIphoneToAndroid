package nl.pvanassen.bplist.parser;

import java.io.UnsupportedEncodingException;

class BPListString implements BPListElement<String> {
    private final String value;
    private final BPListType type;
    
    BPListString(char[] buf) {
        this.value = new String(buf);
        this.type = BPListType.UNICODE_STRING;
    }
    
    BPListString(byte[] buf) throws UnsupportedEncodingException {
        this.value = new String(buf, "ASCII");
        this.type = BPListType.ASCII_STRING;
    }
    
    @Override
    public BPListType getType() {
        return type;
    }
    
    @Override
    public String getValue() {
        return value;
    }
}
