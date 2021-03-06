/**
 * 
 */
package simple.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**Blocks until the list has an item.
 * <br>Created: Feb 8, 2010
 * @author Kenneth Pierce
 */
public class BlockingStack<E> implements BlockingQueue<E> {
	private final Vector<E> list;
	private final boolean fixedSize;
	
	/**
	 * Creates a new fixed size First In Last Out queue of size <code>size</code>.
	 * @param size Size of the queue.
	 */
	public BlockingStack(int size) {
		list = new Vector<E>(size);
		fixedSize = true;
	}
	/**
	 * Creates a new First In Last Out queue with the initial size <code>size</code>.
	 * @param size Initial size of the queue
	 * @param increment Number of slots to add when full. If negative or zero the number of slots double.
	 */
	public BlockingStack(int size, int increment) {
		list = new Vector<E>(size, increment);
		fixedSize = false;
	}
	/** Attempts to add <code>e</code> to the stack, throwing an exception if the stack is full.
	 * @param e The element to add.
	 * @return <code>false</code> if there was an underlying error.
	 * @throws {@link java.lang.IllegalStateException} if the stack is full.
	 * @throws {@link java.lang.NullPointerException} if <code>e</code> is null.
	 * @see java.util.concurrent.BlockingQueue#add(java.lang.Object)
	 * @see #offer(Object)
	 */
	public boolean add(E e) {
		if (isFull())
		{	throw new IllegalStateException("List is full.");	}
		if (e == null)
		{ throw new NullPointerException("Can not add a null element.");	}
		boolean res = list.add(e);
		synchronized (list) {
			list.notifyAll();
		}
		return res;
	}
	
	/** Attempts to add <code>e</code> to the stack.
	 * @param e Element to add.
	 * @return <code>false</code> if stack is full or there was an underlying error.
	 * @throws {@link java.lang.NullPointerException} if <code>e</code> is null
	 * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object)
	 * @see #add(Object)
	 * @see #offer(Object, long, TimeUnit)
	 */
	public boolean offer(E e) {
		if (isFull())
		{	return false;	}
		if (e == null)
		{ throw new NullPointerException("Can not add a null element.");	}
		boolean res = list.add(e);
		synchronized (list) {
			list.notifyAll();
		}
		return res;
	}

	/** Attempts to add <code>e</code> to the stack. Will wait the specified time
	 * if the stack is full.
	 * @param e Element to add.
	 * @param timeout Length of time to wait.
	 * @param unit Units of time to wait.
	 * @return <code>false</code> if the waited time elapsed and the stack is still full or there was an underlying error.
	 * @throws {@link java.lang.InterruptedException} if interrupted
	 * @throws {@link java.lang.NullPointerException} if <code>e</code> is null
	 * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object, long, java.util.concurrent.TimeUnit)
	 */
	public boolean offer(E e, long timeout, TimeUnit unit)
			throws InterruptedException {
		if (isFull())
		{	return false;	}
		if (e == null)
		{ throw new NullPointerException("Can not add a null element.");	}
		synchronized (list) {
			if (isFull()) {
				unit.timedWait(list, timeout);
				if (isFull())
				{	return false;	}
			}
			boolean res = list.add(e);
			list.notifyAll();
			return res;
		}
	}

	/**
	 * Removes and returns the last element added.
	 * Will wait <code>timeout</code> <code>unit</code> if the stack is empty.
	 * @param timeout Amount to wait
	 * @param unit Units to wait
	 * @return The last element added or null if timed out.
	 * @throws {@link java.lang.InterruptedException} if interrupted.
	 * @see java.util.concurrent.BlockingQueue#poll(long, java.util.concurrent.TimeUnit)
	 */
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		synchronized (list) {
			if (list.isEmpty()) {
				unit.timedWait(list, timeout);
				if (list.isEmpty()) {	return null;	}
			}
			E e = list.remove(list.size()-1);
			list.notifyAll();
			return e;
		}
	}

	/**
	 * Adds the element to the list, blocking if the stack is full.
	 * @throws {@link java.lang.InterruptedException} if interrupted.
	 * @see java.util.concurrent.BlockingQueue#put(java.lang.Object)
	 */
	public void put(E e) throws InterruptedException {
		synchronized (list) {
			while(isFull()) {	list.wait(5000);	}
			list.add(e);
			list.notifyAll();
		}
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.BlockingQueue#remove(java.lang.Object)
	 */
	public boolean remove(Object e) {
		boolean res = list.remove(e);
		synchronized (list) {
			list.notifyAll();
		}
		return res;
	}

	/**
	 * Removes and returns the last element added, blocking if the stack is empty.
	 * @return The last element added.
	 * @see java.util.concurrent.BlockingQueue#take()
	 * @see #peek()
	 * @see #poll()
	 * @see #poll(long, TimeUnit)
	 */
	public E take() throws InterruptedException {
		synchronized (list) {
			while (list.isEmpty()) list.wait(5000);
			if (list.isEmpty())
			{	throw new InterruptedException("Interrupted");	}
			E e = list.remove(list.size()-1);
			list.notifyAll();
			return e;
		}
	}

	/**
	 * Returns the last element added or throws an exception if the stack is empty.
	 * @return The last element added.
	 * @throws {@link java.util.NoSuchElementException} if the stack is empty.
	 * @see java.util.Queue#element()
	 * @see #peek()
	 * @see #take()
	 */
	public E element() {
		if (list.isEmpty())
		{	throw new NoSuchElementException("Stack is empty.");	}
		return list.firstElement();
	}

	/**
	 * Returns the last element added or null if the stack is empty.
	 * @return the last element added or null if the stack is empty.
	 * @see java.util.Queue#peek()
	 * @see #element()
	 * @see #take()
	 */
	public E peek() {
		if (list.isEmpty())
		{	return null;	}
		return list.lastElement();
	}

	/**
	 * Removes and returns the last element added or null if the stack is empty.
	 * @return The last element added or null if the stack is empty.
	 * @see java.util.Queue#poll()
	 * @see #remove()
	 * @see #poll(long, TimeUnit)
	 */
	public E poll() {
		if (list.isEmpty())
		{	return null;	}
		return list.remove(list.size()-1);
	}

	/**
	 * Removes and returns the last element added. Throws an exception if the list is empty.
	 * @return The last element added.
	 * @throws {@link java.util.NoSuchElementException} if empty.
	 * @see java.util.Queue#remove()
	 * @see #poll()
	 */
	public E remove() {
		if (list.isEmpty())
		{	throw new NoSuchElementException("List is empty.");	}
		E tmp = list.remove(list.size()-1);
		synchronized (list) {
			list.notifyAll();
		}
		return tmp;
	}
	
	/* ***************************
	 * ***INFORMATION FUNCTIONS***
	 * ***************************/
	/** Returns the remaining capacity of the stack.
	 * @return The remaining capacity of the stack or {@linkplain java.lang.Integer#MAX_VALUE} if the size is not fixed.
	 * @see java.util.concurrent.BlockingQueue#remainingCapacity()
	 */
	public int remainingCapacity() {
		if (fixedSize)
			return list.capacity()-list.size();
		else
			return Integer.MAX_VALUE;
	}
	/**
	 * Whether the stack is full or not.
	 * @return <code>true</code> if the stack is full. Always returns <code>false</code> if the stack is not a fixed size.
	 */
	public boolean isFull() {
		return fixedSize && (list.size() == list.capacity());
	}
	/* (non-Javadoc)
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty() {
		return list.isEmpty();
	}
	
	/* ************************* *
	 * @see java.util.Collection *
	 * ************************* */
	public int size() {
		return list.size();
	}
	public void clear() {
		list.clear();
		synchronized (list) {
			list.notifyAll();
		}
	}
	/**
	 * Adds the collection to the stack if there is enough room.
	 * @return <code>true</code> if all were added, <code>false</code> otherwise.
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends E> c) {
		boolean res = false;
		if (remainingCapacity() <= c.size()) {
			res = list.addAll(c);
			synchronized (list) {
				list.notifyAll();
			}
		}
		return res;
	}

	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	public Iterator<E> iterator() {
		return list.iterator();
	}

	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	public Object[] toArray() {
		return list.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}
	
	/* ************** *
	 * Blah Blah Blah *
	 * ************** */
	/* (non-Javadoc)
	 * @see java.util.concurrent.BlockingQueue#contains(java.lang.Object)
	 */
	public boolean contains(Object e) {
		return list.contains(e);
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection)
	 */
	public int drainTo(Collection<? super E> c) {
		if (c == this)
		{	throw new IllegalArgumentException("Can not add a queue to itself.");	}
		int copied = 0;
		E tmp;
		while ((tmp = this.poll()) != null) {
			if (!c.add(tmp)) {	break;	}
			copied++;
		}
		synchronized (list) {
			list.notifyAll();
		}
		return copied;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.BlockingQueue#drainTo(java.util.Collection, int)
	 */
	public int drainTo(Collection<? super E> c, int max) {
		if (c == this)
		{	throw new IllegalArgumentException("Can not add a queue to itself.");	}
		int copied = 0;
		E tmp;
		for (copied = 0; copied < max; copied++) {
			tmp = this.poll();
			if (tmp == null || !c.add(tmp)) {	break;	}
		}
		synchronized (list) {
			list.notifyAll();
		}
		return copied;
	}
}
