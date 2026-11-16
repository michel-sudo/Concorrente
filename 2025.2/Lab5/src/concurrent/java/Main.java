public class Main {
    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Use: java Main <num_producers> <max_items_per_producer> <producing_time> <num_consumers> <consuming_time>");
            return;
        }
        
        int numProducers = Integer.parseInt(args[0]);
        int maxItemsPerProducer = Integer.parseInt(args[1]);
        int producingTime = Integer.parseInt(args[2]);
        int numConsumers = Integer.parseInt(args[3]);
        int consumingTime = Integer.parseInt(args[4]);
        Consumer[] thConsumers = new Consumer[numConsumers];
        Producer[] thProducers = new Producer[numProducers];
        
        Buffer buffer = new Buffer();

        int i = 0;
        while (i < Math.max(numConsumers, numProducers)){
            if (i < numProducers){
                Producer producer = new Producer(i + 1, buffer, maxItemsPerProducer, producingTime);
                producer.start();
                thProducers[i] = producer;
            }

            if (i < numConsumers) {
                Consumer consumer = new Consumer(i + 1, buffer, consumingTime);
                consumer.start();
                thConsumers[i] = consumer;
            }
            i++;
        }

        for (Producer p : thProducers) {
            try {
                p.join();
            } catch (InterruptedException io) {
                System.err.println();
            }
        }

        for (int j = 0; j < numConsumers; j++) buffer.put(-1);
        
        for (Consumer c : thConsumers) {
            try {
                c.join();
            } catch (InterruptedException io) {
                System.err.println();
            }
        }
    }
}
