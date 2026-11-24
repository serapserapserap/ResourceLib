package serap.io.discovery;

public interface SearchItemProcessor {
    void process(SearchItemStorage storage, SearchItemStorage.SearchItem item) throws UnsupportedOperationException;
}
