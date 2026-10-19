import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class DnaConcurrentMainIA {

    // Classe interna não anônima: tarefa executada por cada thread filha.
    // Cada instância recebe sua PRÓPRIA fatia de arquivos e sua PRÓPRIA
    // posição de escrita no array de resultados — não há posição compartilhada
    // entre threads diferentes.
    private static class CountTask implements Runnable {

        private final File[] files;
        private final int start;
        private final int end; // exclusivo
        private final String pattern;
        private final long[] partialResults; // array compartilhado (escrita)
        private final int resultIndex;       // índice exclusivo desta thread

        public CountTask(File[] files, int start, int end, String pattern,
                          long[] partialResults, int resultIndex) {
            this.files = files;
            this.start = start;
            this.end = end;
            this.pattern = pattern;
            this.partialResults = partialResults;
            this.resultIndex = resultIndex;
        }

        @Override
        public void run() {
            long localTotal = 0;
            try {
                for (int i = start; i < end; i++) {
                    localTotal += countInFile(files[i], pattern);
                }
            } catch (IOException e) {
                System.err.println("[" + Thread.currentThread().getName()
                        + "] Erro ao ler arquivos: " + e.getMessage());
            }
            // Escrita isolada: só esta thread escreve em partialResults[resultIndex].
            partialResults[resultIndex] = localTotal;
        }
    }

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

        // Número de threads: usa os núcleos disponíveis, mas nunca mais threads
        // do que arquivos (não faz sentido criar thread sem trabalho).
        int numThreads = Math.min(
                Runtime.getRuntime().availableProcessors(),
                files.length
        );
        if (numThreads < 1) {
            numThreads = 1;
        }

        Thread[] threads = new Thread[numThreads];
        long[] partialResults = new long[numThreads]; // uma posição por thread

        // Divisão do trabalho: particiona o array de arquivos em numThreads
        // blocos contíguos, tão parecidos em tamanho quanto possível.
        int filesPerThread = files.length / numThreads;
        int remainder = files.length % numThreads;

        int currentStart = 0;
        for (int t = 0; t < numThreads; t++) {
            int sliceSize = filesPerThread + (t < remainder ? 1 : 0);
            int currentEnd = currentStart + sliceSize;

            CountTask task = new CountTask(files, currentStart, currentEnd,
                    pattern, partialResults, t);
            threads[t] = new Thread(task, "Counter-" + t);
            currentStart = currentEnd;
        }

        // Início da execução concorrente
        for (Thread thread : threads) {
            thread.start();
        }

        // Thread principal espera todas as filhas terminarem antes de somar
        // os resultados (happens-before garantido pelo join).
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Execução interrompida.");
            System.exit(5);
        }

        long total = 0;
        for (long partial : partialResults) {
            total += partial;
        }

        System.out.println("Sequência " + pattern + " foi encontrada " + total + " vezes.");
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