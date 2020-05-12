public class Chunk {
    //public final static int CHUNKSIZE = 4096;
    public final static int CHUNK_SIZE = 1048576;

    private byte[] data;
    private long startChunk;
    private int chunkSize;

    public Chunk(byte[] data, long startChunk, int chunkSize) {
        this.data = data;
        this.startChunk = startChunk;
        this.chunkSize = chunkSize;
    }

    public byte[] getData() {
        return this.data;
    }

    public long getStartChunk() {
        return this.startChunk;
    }

    public int getChunkSize() {
        return this.chunkSize;
    }
}
