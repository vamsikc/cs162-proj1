package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;
import java.util.Queue;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	   this.conditionLock = conditionLock;
       waitQueue = new LinkedList<KThread> ();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
    	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

    	conditionLock.release();
        Machine.interrupt().disable();
        KThread currentThread = KThread.currentThread();
        waitQueue.add(currentThread);
        KThread.sleep();
        Machine.interrupt().enable();
    	conditionLock.acquire();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	   Lib.assertTrue(conditionLock.isHeldByCurrentThread());
       Machine.interrupt().disable();
       if (waitQueue.isEmpty()) {
            return;
       }
       KThread next = waitQueue.remove();
       next.ready();
       Machine.interrupt().enable();
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	   Lib.assertTrue(conditionLock.isHeldByCurrentThread());
       Machine.interrupt().disable();
       while (!waitQueue.isEmpty()) {
            waitQueue.remove().ready();
       }
       Machine.interrupt().enable();
    }

    private Lock conditionLock;
    private Queue<KThread> waitQueue;
}
