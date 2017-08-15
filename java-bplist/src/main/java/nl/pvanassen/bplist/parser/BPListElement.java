package nl.pvanassen.bplist.parser;

/**
 * Element types
 * @author Paul van Assen
 *
 * @param <T> Data type in Java language
 */
public interface BPListElement<T> {
    /**
     * 
     * @return List type
     */
    BPListType getType();

    /**
     * @return Actual value
     */
    T getValue();
}
