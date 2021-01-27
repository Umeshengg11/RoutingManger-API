import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class RoutingManagerBuffer {
    public List<File> getInputRoutingBuffer() {
        return inputRoutingBuffer;
    }

    public static List<File> inputRoutingBuffer = new LinkedList<>();
    public static List<File> outputRoutingBuffer = new LinkedList<>();
    private static ReentrantLock inputBufferLock = new ReentrantLock();
    private static ReentrantLock outputBufferLock = new ReentrantLock();
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
        File file = inputRoutingBuffer.get(0);
        inputRoutingBuffer.remove(0);
        inputBufferLock.unlock();
        return file;
    }
}
