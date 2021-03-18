package main;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is used to create object of RoutingManagerBuffer.
 * There are inputRoutingBuffer and outputRoutingBuffer.
 */
class RoutingManagerBuffer {

    private static final List<File> inputRoutingBuffer = new LinkedList<>();
    private static final List<File> outputRoutingBuffer = new LinkedList<>();
    private static final ReentrantLock inputBufferLock = new ReentrantLock();
    private static final ReentrantLock outputBufferLock = new ReentrantLock();
    private static RoutingManagerBuffer routingManagerBuffer;
    private static final Logger log = Logger.getLogger(RoutingManagerBuffer.class);

    /**
     * This is the default constructor of the class.
     * However this is made private so that it cannot be accessed from outside the class.
     */
    private RoutingManagerBuffer() {
    }

    /**
     * @return - Object of RoutingManagerBuffer.
     * This is made singleton object as only one instance can be accessed.
     */
    public static RoutingManagerBuffer getInstance() {
        if (routingManagerBuffer == null) {
            routingManagerBuffer = new RoutingManagerBuffer();
        }
        return routingManagerBuffer;
    }

    /**
     * @return - object of inputRoutingBuffer
     */
    private List<File> getInputRoutingBuffer() {
        return inputRoutingBuffer;
    }

    /**
     * @param file - The File object is given as input argument.
     * @return - boolean value true if the file is added successfully.
     */
    boolean addToInputBuffer(File file) {
        inputBufferLock.lock();
        inputRoutingBuffer.add(file);
        log.debug("File added to Input buffer");
        inputBufferLock.unlock();
        return true;
    }

    /**
     * This method is used to fetch file from the inputBuffer one by one.
     * @return - File
     */
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

    /**
     * @param file - File object is given as input argument.
     * @return - true if the file is added successfully.
     */
    boolean addToOutputBuffer(File file) {
        outputBufferLock.lock();
        outputRoutingBuffer.add(file);
        log.debug("File added to Output buffer");
        outputBufferLock.unlock();
        return true;
    }

    /**
     * This method is used to fetch file from the outputBuffer.
     * @return - File.
     */
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
