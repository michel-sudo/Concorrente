import java.util.concurrent.Semaphore;

class Buffer {
    private static final int CAPACITY = 50;
    private final int[] data = new int[CAPACITY];
    private int in = 0, out = 0, size = 0;
    private final Semaphore empty = new Semaphore(CAPACITY); // espacos livres
    private final Semaphore full = new Semaphore(0);         // itens disponiveis
    private final Semaphore mutex = new Semaphore(1);        // exclusao mutua

    public void put(int value) throws InterruptedException {
        empty.acquire();
        mutex.acquire();
        data[in] = value;
        in = (in + 1) % CAPACITY;
        size++;
        if (value != -1) System.out.println("Inserted: " + value + " | Buffer size: " + size);
        mutex.release();
        full.release();
    }

    public int remove() throws InterruptedException {
        full.acquire();
        mutex.acquire();
        int value = data[out];
        out = (out + 1) % CAPACITY;
        size--;
        if (value != -1) System.out.println("Removed: " + value + " | Buffer size: " + size);
        mutex.release();
        empty.release();
        return value;
    }
}
