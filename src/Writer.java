import java.lang.InterruptedException;
import java.io.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Writer implements Runnable {
    private File downloadedFile;
    private File metadataFile;
    private File tempMetadataFile;
    private int numberOfChunksWritten;

    public Writer(int numberOfChunkWritten) {
        this.numberOfChunksWritten = numberOfChunkWritten;
        this.downloadedFile = new File(System.getProperty("user.dir") + '/' + Manager.fileName);
        this.tempMetadataFile = new File(  Manager.tempMetadataPath );
        this.metadataFile = new File(Manager.metadataPath);
    }

    public void run() {
        try {
            RandomAccessFile file = new RandomAccessFile(this.downloadedFile, "rw");
            //FileOutputStream outputStream = new FileOutputStream(this.downloadedFile, true);
            int downloadPercentage = 0;
            while (this.numberOfChunksWritten != Manager.numberOfChunks) {
                Chunk currentChunk = Manager.blocksQueue.take();
                int chunkIndex = (int)currentChunk.getStartChunk()/Chunk.CHUNK_SIZE;
                file.seek(currentChunk.getStartChunk());
                file.write(currentChunk.getData(), 0, currentChunk.getChunkSize());
                Manager.metadata[chunkIndex] = true;
                this.numberOfChunksWritten++;
                metadataSerialization();

                int lastDownloadPercentage = downloadPercentage;
                downloadPercentage = (int) Math.ceil(((double) numberOfChunksWritten / Manager.metadata.length) * 100);
                if(downloadPercentage != lastDownloadPercentage) {
                    System.out.println("Downloaded " + downloadPercentage + "%");
                }
            }
            //outputStream.close();
            file.close();
            this.metadataFile.delete();
            this.tempMetadataFile.delete();
            System.out.println("Download succeed");
        } catch (InterruptedException interruptedException) {
            System.err.println("InterruptedException: " + interruptedException.toString());
        } catch (IOException IOException) {
            System.err.println("IOException: " + IOException.toString());
        }
    }

    private void metadataSerialization() {
        try {
            FileOutputStream metadataOut = new FileOutputStream(this.tempMetadataFile);
            ObjectOutputStream objMetadataOut = new ObjectOutputStream(metadataOut);
            objMetadataOut.writeObject(Manager.metadata);
            objMetadataOut.close();
            metadataOut.close();
            Path tempPath = this.tempMetadataFile.toPath();
            Path origPath = this.metadataFile.toPath();
            Files.move(tempPath, origPath,REPLACE_EXISTING, ATOMIC_MOVE);

        } catch (IOException IOException){
            System.err.println("IOException: " + IOException.toString());
        }
    }
}