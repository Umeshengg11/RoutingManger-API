package main;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

class RoutingManagerBuffer {

    private static final List<File> inputRoutingBuffer = new LinkedList<>();
    private static final List<File> outputRoutingBuffer = new LinkedList<>();
    private static final ReentrantLock inputBufferLock = new ReentrantLock();
    private static final ReentrantLock outputBufferLock = new ReentrantLock();
    private static RoutingManagerBuffer routingManagerBuffer;
    private static final Logger log = Logger.getLogger(RoutingManagerBuffer.class);

    private RoutingManagerBuffer() {
    }

    public static RoutingManagerBuffer getInstance() {
        if (routingManagerBuffer == null) {
            routingManagerBuffer = new RoutingManagerBuffer();
        }
        return routingManagerBuffer;
    }
    private List<File> getInputRoutingBuffer() {
        return inputRoutingBuffer;
    }

    boolean addToInputBuffer(File file) {
        inputBufferLock.lock();
        inputRoutingBuffer.add(file);
        log.debug("File added to Input buffer");
        inputBufferLock.unlock();
        return true;
    }

    File fetchFromInputBuffer() {
        inputBufferLock.lock();
        File file = null;
        try{
            file = inputRoutingBuffer.get(0);
            inputRoutingBuffer.remove(0);
        } catch(Exception e) {
            log.debug("There is no File in Input Buffer");
        }
        inputBufferLock.unlock();
        return file;
    }

    boolean addToOutputBuffer(File file) {
        outputBufferLock.lock();
        outputRoutingBuffer.add(file);
        log.debug("File added to Output buffer");
        outputBufferLock.unlock();
        return true;
    }

    File fetchFromOutputBuffer() {
        outputBufferLock.lock();
        File file = null;
        try{
            file = outputRoutingBuffer.get(0);
            outputRoutingBuffer.remove(0);
        } catch(Exception e) {
            log.debug("There is no File in Output Buffer");
        }
        outputBufferLock.unlock();
        return file;
    }
}
