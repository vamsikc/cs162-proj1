package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.LinkedList;
import java.util.HashMap;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
			       
		return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
			       
		return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
			       
		Lib.assertTrue(priority >= priorityMinimum &&
			   priority <= priorityMaximum);
		
		getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
			       
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
		    return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
    }

    public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
			       
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
		    return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
		    thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
    	private KThread owner;
		PriorityQueue(boolean transferPriority) {
		    this.transferPriority = transferPriority;
		    waitingThreads = new java.util.PriorityQueue<ThreadState> ();
		    owner = null;
		}

		public void waitForAccess(KThread thread) {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    // implement me
		    ThreadState next = pickNextThread();
		    if (next == null) {
		    	owner = null;
		    	return null;
		    }
		    if (owner != null) {
		    	ThreadState ownerState = getThreadState(owner);
				Queue<PriorityQueue> ownerResources = ownerState.acquiredResources;
				if (ownerResources != null && !ownerResources.isEmpty()) {
					ownerResources.remove(this);
					ownerState.resourceMap.remove(this);
				}
			}
	    	this.acquire(next.thread);
		    waitingThreads.remove();
		    return next.thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected ThreadState pickNextThread() {
		    if (waitingThreads.isEmpty()) {
		    	return null;
		    }

		    return waitingThreads.peek();
		}
		
		public void print() {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    // implement me (if you want)
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		private java.util.PriorityQueue<ThreadState> waitingThreads;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState implements Comparable{
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param	thread	the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
		    this.thread = thread;
		    
		    setPriority(priorityDefault);
		    effectivePriority = this.priority;
		    acquiredResources = new LinkedList<PriorityQueue>();
		    resourceMap = new HashMap<PriorityQueue, Integer> ();
		    timeQueued = 0;
		    waitingOn = null;
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return	the priority of the associated thread.
		 */
		public int getPriority() {
		    return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			effectivePriority = this.priority;
		    for (PriorityQueue p : acquiredResources) {
		    	Integer value = resourceMap.get(p);
		    	if (value == null) {
		    		value = effectiveHelper(p);
		    		resourceMap.put(p, new Integer(value));
		    	}
		    	effectivePriority = Math.max(value, effectivePriority);
		    }
		    return effectivePriority;
		}

		public int effectiveHelper(PriorityQueue p) {
			int maxP = 0;
			for (ThreadState s: p.waitingThreads) {
		    	maxP = Math.max(maxP, s.getEffectivePriority());
		    }
		    return maxP;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority) {
		    if (this.priority == priority)
				return;

		    // implement me
		    int p = Math.max(priority, PriorityScheduler.priorityMinimum);
		    p = Math.min(priority, PriorityScheduler.priorityMaximum);
		    this.priority = p;
		    if (this.waitingOn == null) {
		    	return;
		    }
		    backwardPriorityHelper(this.waitingOn, this.priority);
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			this.timeQueued = Machine.timer().getTime();
		    waitQueue.waitingThreads.add(this);
		    waitingOn = waitQueue;
		    KThread owner = waitQueue.owner;
		    if (owner == null) {
		    	return;
		    }
			ThreadState state = getThreadState(owner);
		    HashMap<PriorityQueue, Integer> map = state.resourceMap;
		    if (map.keySet().size() == 0) {
		    	return;
		    }
	    	int temp = map.get(waitQueue);
		    int newP = this.getEffectivePriority();
		    if (newP > temp) {
		    	state.resourceMap.put(waitQueue, newP);
		    	if (state.waitingOn == null) {
		    		return;
		    	}
		    	backwardPriorityHelper(state.waitingOn, newP);
		    }
		}

		public void backwardPriorityHelper(PriorityQueue waiting, int newPriority) {
			KThread owner = waiting.owner;
			if (owner == null) {
				return;
			}
			ThreadState state = getThreadState(owner);
			HashMap<PriorityQueue, Integer> map = state.resourceMap;
			if (map.keySet().size() == 0) {
		    	return;
		    }
		    int temp = map.get(waiting);
			if (newPriority > temp) {
				state.resourceMap.put(waiting, newPriority);
				if (state.waitingOn == null) {
					return;
				}
				backwardPriorityHelper(state.waitingOn, newPriority);
			}
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
		    // implement me
		    waitQueue.owner = this.thread;
		    acquiredResources.add(waitQueue);
		}	

		public int compareTo(Object t) {
			ThreadState a = (ThreadState) t;
			if (this.getPriority() < a.getPriority()) {
				return 1;
			} else if (this.getPriority() > a.getPriority()) {
				return -1;
			} else {
				if (this.timeQueued > a.timeQueued) {
					return 1;
				} else if (this.timeQueued < a.timeQueued) {
					return -1;
				}
			}
			return 0;
		}

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		protected int effectivePriority;
		protected Queue<PriorityQueue> acquiredResources;
		protected HashMap<PriorityQueue, Integer> resourceMap;
		protected long timeQueued;
		protected PriorityQueue waitingOn;
    }
}
