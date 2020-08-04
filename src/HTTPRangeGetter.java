import java.net.*;
import java.io.*;
import java.io.IOException;

public class HTTPRangeGetter implements Runnable {
    private long startBlock;
    private long endBlock;
    private URL url;

    public HTTPRangeGetter(long startBlock, long endBlock, URL url) {
        this.startBlock = startBlock;
        this.endBlock = endBlock;
        this.endBlock = endBlock;
        this.url = url;
    }

    public void run() {
        try {
            int indexOfUnwritten = (int)startBlock/Chunk.CHUNK_SIZE;
            while(indexOfUnwritten*Chunk.CHUNK_SIZE < endBlock) {
                if(Manager.metadata[indexOfUnwritten]) {
                    indexOfUnwritten++;
                } else {
                    break;
                }
            }

            long updateStartBlock = indexOfUnwritten*Chunk.CHUNK_SIZE;
            boolean isFinished = updateStartBlock > endBlock;
            if(!isFinished) {
                HttpURLConnection connection = (HttpURLConnection) this.url.openConnection();
                connection.setRequestProperty("Range", "bytes=" + updateStartBlock + "-" + this.endBlock);
                connection.connect();
                connection.setReadTimeout(6000); // for network failure handling - 1 min waiting

                InputStream inputStream = connection.getInputStream();
                long currentLocation = this.startBlock;
                while (currentLocation < this.endBlock) {
                    int currentIndex = (int) currentLocation / Chunk.CHUNK_SIZE;
                    int chunkSize = currentIndex != Manager.numberOfChunks-1 ?
                            Chunk.CHUNK_SIZE : (int) (this.endBlock - currentLocation + 1);

                    byte[] buffer = new byte[chunkSize];
                    inputStream.readNBytes(buffer, 0, chunkSize);
                    currentLocation += chunkSize;

                    if (!Manager.metadata[currentIndex]) {
                        Chunk currentChunk = new Chunk(buffer, currentLocation, chunkSize);
                        Manager.blocksQueue.put(currentChunk);
                        currentLocation += chunkSize;
                        inputStream.skip(chunkSize);
                        continue;
                    }
                    currentLocation += chunkSize;
                    Chunk currentChunk = new Chunk(buffer, currentLocation, chunkSize);
                    Manager.blocksQueue.put(currentChunk);
                    currentLocation += chunkSize;
                }
                inputStream.close();
                connection.disconnect();
            }
        } catch (IOException ioException) {
            System.err.println("IOException: " + ioException.toString());
            System.exit(0);
        } catch (InterruptedException interruptedException) {
            System.err.println("InterruptedException: " + interruptedException.toString());
            System.exit(0);
        }
    }
}