public class Main {
    public static void main(String[] args) throws InterruptedException {
        if (args.length != 5) {
            System.out.println("Use: java Main <num_producers> <max_items_per_producer> <producing_time> <num_consumers> <consuming_time>");
            return;
        }

        int numProducers = Integer.parseInt(args[0]);
        int maxItemsPerProducer = Integer.parseInt(args[1]);
        int producingTime = Integer.parseInt(args[2]);
        int numConsumers = Integer.parseInt(args[3]);
        int consumingTime = Integer.parseInt(args[4]);

        Buffer buffer = new Buffer();
        Producer[] producers = new Producer[numProducers];
        Consumer[] consumers = new Consumer[numConsumers];

        for (int i = 0; i < numProducers; i++) {
            producers[i] = new Producer(i + 1, buffer, maxItemsPerProducer, producingTime);
            producers[i].start();
        }
        for (int i = 0; i < numConsumers; i++) {
            consumers[i] = new Consumer(i + 1, buffer, consumingTime);
            consumers[i].start();
        }

        for (Producer p : producers) p.join();
        // um sinal de fim (-1) por consumidor, apos todos os itens produzidos
        for (int i = 0; i < numConsumers; i++) buffer.put(-1);
        for (Consumer c : consumers) c.join();
    }
}
