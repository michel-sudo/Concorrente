import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.Math;

public class DnaConcurrentMain {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java DnaSerialMain DIRETORIO_ARQUIVOS PADRAO");
            System.err.println("Exemplo: java DnaSerialMain dna_inputs CGTAA");
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

        int numThreads = (int) (Math.log10(files.length) / Math.log10(2));
        int filesPerThread = Math.ceilDiv(files.length, numThreads);
        long[] totalGlobal = new long[numThreads];
        Thread[] threads = new Thread[numThreads];
        int idxThreads = 0;

        try {
            int idxFiles = 0; // Index dos arquivos no array orginal
            for (int j = 1; j <= numThreads; j++){
                File[] subFiles = new File[filesPerThread]; // Array para subdivisão dos arquivos
                int idxSubFiles = 0; // Index do array de subdivisão
                
                // Divide os arquivos em sub arrays, que serão passados para as threads
                for (; idxSubFiles < filesPerThread && idxFiles < files.length; idxSubFiles++) {
                    subFiles[idxSubFiles] = files[idxFiles];
                    idxFiles += 1;                    
                }

                Work work = new Work(subFiles, pattern, totalGlobal, idxThreads);
                Thread th = new Thread(work);
                threads[idxThreads] = th;
                idxThreads += 1;
                th.start();
            }

            for (Thread t : threads) {
                t.join();
            }

        } catch (InterruptedException i) {
            System.err.println(i.getMessage());
        }

        long somaTotal = 0;
        for (long t : totalGlobal) {
            somaTotal += t;
        }

        System.out.println("Sequência " + pattern + " foi encontrada " + somaTotal + " vezes.");
    }

    private static class Work implements Runnable {

        private int localTotal = 0;
        private File[] files;
        private String pattern;
        private long[] globalTotal;
        private int index;

        public Work(File[] files, String pattern, long[] globalTotal, int index) {
            this.files = files;
            this.pattern = pattern;
            this.globalTotal = globalTotal;
            this.index = index;
        }

        @Override
        public void run(){

            try {
                for (File f : this.files){
                    if (f != null) {
                        this.localTotal += countInFile(f, this.pattern);
                    }
                }
                globalTotal[index] = this.localTotal;

            } catch (IOException e) {
                System.err.println("Erro ao ler arquivos: " + e.getMessage());
                System.exit(4);
            }
        }
    } 

    public static long countInFile(File file, String pattern) throws IOException {
        long total = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
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
