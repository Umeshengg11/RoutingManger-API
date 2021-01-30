import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class RoutingManagerBuffer {
    private List<File> getInputRoutingBuffer() {
        return inputRoutingBuffer;
    }

    private static final List<File> inputRoutingBuffer = new LinkedList<>();
    private static final List<File> outputRoutingBuffer = new LinkedList<>();
    private static final ReentrantLock inputBufferLock = new ReentrantLock();
    private static final ReentrantLock outputBufferLock = new ReentrantLock();
    private static RoutingManagerBuffer routingManagerBuffer;

    private RoutingManagerBuffer() {
    }

    public static RoutingManagerBuffer getInstance() {
        if (routingManagerBuffer == null) {
            routingManagerBuffer = new RoutingManagerBuffer();
        }
        return routingManagerBuffer;
    }

    public boolean addFileToBuffer(File file) {
        inputBufferLock.lock();
        inputRoutingBuffer.add(file);
        System.out.println("file added to the buffer");
        System.out.println(inputRoutingBuffer.size());
        inputBufferLock.unlock();
        return true;
    }

    public File fetchFileFromBuffer() {
        inputBufferLock.lock();
        File file = null;
        try{
            file = inputRoutingBuffer.get(0);
            inputRoutingBuffer.remove(0);
        } catch(Exception e) {
            System.out.println("There is no File in Input Buffer");
        }
        inputBufferLock.unlock();
        return file;
    }
}
