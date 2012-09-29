package nachos.threads;

import nachos.machine.*;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Iterator;
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	   Machine.timer().setInterruptHandler(new Runnable() {
		  public void run() { timerInterrupt(); }
	   });
       waitQueue = new LinkedList<ThreadObject> ();
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        Machine.interrupt().disable();
        Iterator<ThreadObject> iter = waitQueue.iterator();
        while (iter.hasNext()) {
            ThreadObject nxt = iter.next();
            if (nxt.wakeTime <= Machine.timer().getTime()) {
                iter.remove();
                nxt.thread.ready();
            }
        }
        Machine.interrupt().enable();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        // for now, cheat just to get something working (busy waiting is bad)
        if (x == 0) {
            return;
        }
        Machine.interrupt().disable();
        long wakeTime = Machine.timer().getTime() + x;
        KThread curr = KThread.currentThread();
        waitQueue.add(new ThreadObject(curr, wakeTime));
        KThread.sleep();
        Machine.interrupt().enable();
    }

    private Queue<ThreadObject> waitQueue;
}

class ThreadObject {
    KThread thread;
    long wakeTime;
    public ThreadObject() {
        wakeTime = 0;
        thread = null;
    }
    public ThreadObject(KThread t0, long time) {
        wakeTime = time;
        thread = t0;
    }
}