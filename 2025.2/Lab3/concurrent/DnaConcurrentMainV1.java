import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class DnaConcurrentMainV1 {

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

        long[] totalGlobal = new long[files.length];
        Thread[] threads = new Thread[files.length];
        int index = 0;

        try {
            for (File f : files) {
                Work work = new Work(f, pattern, totalGlobal, index);
                Thread th = new Thread(work);
                threads[index] = th;
                index += 1;
                th.start();
            }

            for (Thread t : threads) {
                t.join();
            }

        } catch (IOException e) {
            System.err.println("Erro ao ler arquivos: " + e.getMessage());
            System.exit(4);
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
        private File file;
        private String pattern;
        private long[] globalTotal;
        private int index;

        public Work(File file, String pattern, long[] globalTotal, int index) throws IOException {
            this.file = file;
            this.pattern = pattern;
            this.globalTotal = globalTotal;
            this.index = index;
        }

        @Override
        public void run() {

            try {
                localTotal += countInFile(this.file, this.pattern);
                globalTotal[index] = localTotal;

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