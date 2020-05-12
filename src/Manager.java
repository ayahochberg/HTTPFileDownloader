import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.net.MalformedURLException;
import java.io.*;

public class Manager {
    private int numberOfConnection;
    private String inputURL;
    private List<URL> urls;
    private  File metadataFile;
    public static boolean[] metadata;
    public static BlockingQueue<Chunk> blocksQueue;
    public static int totalWrittenBytes;
    public static String fileName;
    public static String metadataPath;
    public static String tempMetadataPath;
    public static int fileLength;
    public static int numberOfChunks;

    public Manager(String url, int numberOfThreads) {
        this.inputURL = url;
        this.urls = new ArrayList<>();
        this.numberOfConnection = numberOfThreads;
        this.blocksQueue = new LinkedBlockingQueue<>();
        this.fileName = getFileName(url);
        this.metadataPath = "metadata_" + this.fileName.substring(0, this.fileName.lastIndexOf('.'));
        this.tempMetadataPath = "temp_metadata_" + this.fileName.substring(0, this.fileName.lastIndexOf('.'));
        this.metadataFile = new File(metadataPath);
        this.totalWrittenBytes = 0;
    }

    public void initURL() {
        try {
            List<String> stringURLs = new ArrayList<>();
            boolean isSingleUrl = this.inputURL.contains("http");
            stringURLs.add(inputURL);
            stringURLs = isSingleUrl ? stringURLs : Files.readAllLines(Paths.get(inputURL));
            for(int i = 0; i < stringURLs.size(); i++) {
                URL url = new URL(stringURLs.get(i));
                this.urls.add(url);
            }
            HttpURLConnection connection = (HttpURLConnection) this.urls.get(0).openConnection();
            connection.connect();
            this.fileLength = connection.getContentLength();
            this.numberOfConnection = this.fileLength < 20 ? 1 : this.numberOfConnection;
            this.numberOfChunks = this.fileLength % Chunk.CHUNK_SIZE == 0 ?
                    this.fileLength / Chunk.CHUNK_SIZE : this.fileLength / Chunk.CHUNK_SIZE + 1;
            this.metadata = new boolean[numberOfChunks];
            startDownload();
        } catch (MalformedURLException malformedURLException) {
            System.err.println("MalformedURLException: " + malformedURLException.toString());
        } catch (IOException IOException) {
            System.out.println("IOException: " + IOException.toString());
        }
    }

    public void startDownload() {
        int numberOfChunksWritten = 0;
        if (new File(this.metadataPath).exists()) {
            setupResume();
            for (int i = 0; i < this.metadata.length; i++) {
                if (metadata[i]) numberOfChunksWritten++;
            }
        }

        int blockSize = (this.numberOfChunks / this.numberOfConnection) * Chunk.CHUNK_SIZE;
        for(int i = 0; i < this.numberOfConnection; i++) {
            long startBlock =  blockSize*i;
            long endBlock = (i == this.numberOfConnection -1) ? this.fileLength-1 : startBlock + (blockSize)-1;
            Random random = new  Random();
            URL randomUrl = this.urls.get(random.nextInt(this.urls.size()));
            Thread thread =  new Thread(new HTTPRangeGetter(startBlock, endBlock, randomUrl));
            thread.start();
        }

        Writer writer = new Writer(numberOfChunksWritten);
        Thread writerThread = new Thread(writer);
        writerThread.start();
    }

    public void setupResume() {
        try {
            FileInputStream metadataToUpdate = new FileInputStream(this.metadataFile);
            ObjectInputStream objMetadataIn = new ObjectInputStream(metadataToUpdate);
            this.metadata = (boolean[]) objMetadataIn.readObject();
            metadataToUpdate.close();
            objMetadataIn.close();
        } catch (IOException | ClassNotFoundException exception) {
            System.err.println("Exception: " + exception.toString());
        }
    }

    private static String getFileName(String url) {
        return url.contains("http") ? url.substring(url.lastIndexOf('/') + 1)
                : url.substring(0, url.lastIndexOf('.'));
    }
}