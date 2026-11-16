import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Sum {

    public static int sum(FileInputStream fis) throws IOException {
        
	int byteRead;
        int sum = 0;
        
        while ((byteRead = fis.read()) != -1) {
        	sum += byteRead;
        }

        return sum;
    }

    public static long sum(String path) throws IOException {

        Path filePath = Paths.get(path);
        if (Files.isRegularFile(filePath)) {
       	    FileInputStream fis = new FileInputStream(filePath.toString());
            return sum(fis);
        } else {
            throw new RuntimeException("Non-regular file: " + path);
        }
    }

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.err.println("Usage: java Sum filepath1 filepath2 filepathN");
            System.exit(1);
        }

        long[] results = new long[args.length];
        Thread[] threads = new Thread[args.length];
        for (int i = 0; i < args.length; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                try {
                    results[idx] = sum(args[idx]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i] + " : " + results[i]);
        }
    }
}
