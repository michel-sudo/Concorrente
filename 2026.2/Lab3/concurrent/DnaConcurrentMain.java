import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class DnaConcurrentMain {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java DnaConcurrentMain DIRETORIO_ARQUIVOS PADRAO");
            System.err.println("Exemplo: java DnaConcurrentMain dna_inputs CGTAA");
            System.exit(1);
        }

        String dirName = args[0];
        String pattern = args[1];

        File dir = new File(dirName);
        if (!dir.isDirectory()) {
            System.err.println("Caminho não é um diretório: " + dirName);
            System.exit(2);
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.err.println("Nenhum arquivo .txt encontrado em: " + dirName);
            System.exit(3);
        }

        try {
            //long total = 0;
            int index = 0;
            Thread[] threads = new Thread[files.length];
            long[] totais = new long[files.length];
            
	    for (File f : files) {
                Work work = new Work(f, pattern, index, totais);
                Thread t = new Thread(work);
                threads[index] = t;
                t.start();
                index++;
                //total += countInFile(f, pattern);
            }

        for (Thread t : threads) {
            t.join();
        }

        long somaTotal = 0;
        for (long elemento : totais) {
            somaTotal += elemento;
        }

	    System.out.println("Sequência " + pattern + " foi encontrada " + somaTotal + " vezes.");
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivos: " + e.getMessage());
            System.exit(4);
        } catch (InterruptedException i) {
            System.err.println(i);
        }
    }


    private static class Work implements Runnable {
        
        private long totalLocal;
        private int index;
        private long[] totais;
        private File file;
        private String pattern;

        public Work(File file, String pattern, int index, long[] totais) throws IOException{
            this.file = file;
            this.index = index;
            this.totais = totais;
            this.totalLocal = 0;
            this.pattern = pattern;
        }  

        @Override
        public void run() {
            try {
                totalLocal = countInFile(this.file, this.pattern);
                totais[this.index] = totalLocal;
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    public static long countInFile(File file, String pattern) throws IOException {
        long total = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                
		// simulando delay de processamento
		try {
             	   Thread.sleep(10);
           	} catch (InterruptedException e) {
                   Thread.currentThread().interrupt();
                }

		if (!line.isEmpty()) {
                    total += countInSequence(line, pattern);
                }
            }
        }
        return total;    
    }

    public static long countInSequence(String sequence, String pattern) {
        if (sequence == null || pattern == null) {
            return 0;
        }
        int n = sequence.length();
        int m = pattern.length();
        if (m == 0 || n < m) {
            return 0;
        }
        long count = 0;
        for (int i = 0; i <= n - m; i++) {
            if (sequence.regionMatches(false, i, pattern, 0, m)) {
                count++;
            }
        }
        return count;
    }

}
