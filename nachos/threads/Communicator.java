package nachos.threads;

import nachos.machine.*;

import java.util.Queue;
import java.util.LinkedList;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        mutex = new Lock();
        okToListen = new Condition2(mutex);
        okToSpeak = new Condition2(mutex);
        isReceived = new Condition2(mutex);
        message = 0;
        bufferFull = false;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param   word    the integer to transfer.
     */
    public void speak(int word) {
        mutex.acquire();
        while (bufferFull) {
            okToSpeak.sleep();
        }
        message = word;
        bufferFull = true;
        okToListen.wake();
        isReceived.sleep();
        mutex.release(); 
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return  the integer transferred.
     */    
    public int listen() {
        mutex.acquire();
        while (!bufferFull) {
            okToListen.sleep();
        }
        int val = message;
        bufferFull = false;
        message = null;
        isReceived.wake();
        okToSpeak.wake();
        mutex.release();
        return val;
    }
    private Lock mutex;
    private Condition2 okToListen;
    private Condition2 okToSpeak;
    private Condition2 isReceived;
    private Integer message;
    private boolean bufferFull;
}