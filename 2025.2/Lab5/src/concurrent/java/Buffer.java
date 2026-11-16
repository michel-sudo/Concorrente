import java.util.concurrent.Semaphore;
import java.util.Queue;
import java.util.ArrayDeque;

class Buffer {
    private final Queue<Integer> data = new ArrayDeque<>();

    Semaphore mutex = new Semaphore(1);
    Semaphore full = new Semaphore(50);
    Semaphore empty = new Semaphore(0);
    
    public void put(int value) {
        try {
            full.acquire();
            mutex.acquire();
            data.add(value);
            if (value != -1) System.out.println("Inserted: " + value + " | Buffer size: " + data.size());
            mutex.release();
            empty.release();
        } catch (InterruptedException i) {
            System.err.println();
        }
    }
    
    public int remove() {
        try {
            empty.acquire();
            mutex.acquire();
            int value = data.remove();
            if (value != -1) System.out.println("Removed: " + value + " | Buffer size: " + data.size());
            mutex.release();
            full.release();
            return value;
        } catch (InterruptedException i) {
            System.err.println();
        }
        return -1;
    }
}
