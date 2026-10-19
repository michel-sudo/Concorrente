import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lab 3B — Pipeline Concorrente de Processamento de Imagens
 *
 * Arquitetura: dois estágios ligados por filas BlockingQueue.
 *
 *   [Produtor]──▶ loadQueue ──▶ [Workers (pool)]──▶ saveQueue ──▶ [Salvar/Metadados]
 *
 * Estágio 1 — Produtor (1 thread): simula carregamento de imagem (I/O-bound).
 *   Publica ImageTask na loadQueue e encerra com N tokens "veneno" para sinalizar
 *   o fim aos workers.
 *
 * Estágio 2 — Thread Pool (N threads): cada worker retira uma ImageTask da
 *   loadQueue, aplica todos os filtros e redimensionamentos (CPU-bound), e
 *   empurra o resultado processado na saveQueue.
 *
 * Estágio 3 — Consumidor (1 thread): retira de saveQueue, simula escrita em
 *   disco e geração de metadados (I/O-bound). Encerra ao receber 1 token veneno.
 *
 * Sincronização:
 *   - BlockingQueue (LinkedBlockingQueue) é thread-safe internamente.
 *     Não há acesso direto a estado compartilhado mutável entre threads.
 *   - AtomicInteger/AtomicLong para contadores de estatísticas globais.
 *   - Padrão "poison pill" para encerramento gracioso sem flags booleanas voláteis.
 */
public class ImagePipelineMain {

    // ── Constantes de tempo simulado (milissegundos) ─────────────────────────
    static final int T_LOAD          = 200; // carregar imagem do disco
    static final int T_GRAYSCALE     = 50;  // filtro escala de cinza
    static final int T_BLUR          = 75;  // desfoque
    static final int T_EDGE          = 60;  // detecção de bordas
    static final int T_THUMB         = 40;  // redimensionar → thumbnail
    static final int T_WEB           = 45;  // redimensionar → web
    static final int T_PRINT         = 50;  // redimensionar → impressão
    static final int T_SAVE          = 150; // salvar arquivos em disco
    static final int T_METADATA      = 20;  // gerar metadados

    // Capacidade máxima de cada fila (backpressure: evita que o produtor
    // sobrecarregue a memória se os workers forem mais lentos).
    static final int QUEUE_CAPACITY  = 20;

    // Token sentinela ("poison pill"): valor inválido que sinaliza fim de stream.
    static final ImageTask POISON    = new ImageTask(-1);

    // ── Estatísticas globais ─────────────────────────────────────────────────
    static final AtomicInteger imagesProcessed = new AtomicInteger(0);
    static final AtomicLong    totalSavedBytes = new AtomicLong(0);

    // ── Modelo de dados ──────────────────────────────────────────────────────

    /** Representa uma imagem em trânsito pelo pipeline. */
    static class ImageTask {
        final int id;
        boolean grayscaleDone;
        boolean blurDone;
        boolean edgeDone;
        boolean thumbDone;
        boolean webDone;
        boolean printDone;

        ImageTask(int id) { this.id = id; }

        boolean isPoison() { return id == -1; }
    }

    // ── Estágio 1: Produtor ──────────────────────────────────────────────────

    /**
     * Produtor: simula leitura de imagens do disco e as enfileira.
     * I/O-bound: dorme T_LOAD ms por imagem.
     * Ao final, injeta numWorkers poison pills para encerrar todos os workers.
     */
    static class ImageLoader implements Runnable {
        private final int totalImages;
        private final int numWorkers;
        private final BlockingQueue<ImageTask> loadQueue;

        ImageLoader(int totalImages, int numWorkers, BlockingQueue<ImageTask> loadQueue) {
            this.totalImages = totalImages;
            this.numWorkers  = numWorkers;
            this.loadQueue   = loadQueue;
        }

        @Override
        public void run() {
            for (int i = 0; i < totalImages; i++) {
                sleep(T_LOAD); // simula I/O de disco
                try {
                    // put() bloqueia se a fila estiver cheia (backpressure automático).
                    loadQueue.put(new ImageTask(i));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                System.out.printf("[Loader] Imagem %d carregada%n", i);
            }
            // Injeta um poison pill por worker para encerramento gracioso.
            for (int w = 0; w < numWorkers; w++) {
                try { loadQueue.put(POISON); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // ── Estágio 2: Worker (thread pool) ─────────────────────────────────────

    /**
     * Worker: consome ImageTask da loadQueue, aplica filtros e redimensionamentos
     * (CPU-bound), publica resultado na saveQueue.
     * Cada worker termina ao receber seu poison pill.
     */
    static class ImageProcessor implements Runnable {
        private final BlockingQueue<ImageTask> loadQueue;
        private final BlockingQueue<ImageTask> saveQueue;

        ImageProcessor(BlockingQueue<ImageTask> loadQueue, BlockingQueue<ImageTask> saveQueue) {
            this.loadQueue = loadQueue;
            this.saveQueue = saveQueue;
        }

        @Override
        public void run() {
            while (true) {
                ImageTask task;
                try {
                    // take() bloqueia até haver item disponível — sem busy-wait.
                    task = loadQueue.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                // Poison pill: encerra este worker sem processar nada.
                if (task.isPoison()) return;

                // Filtros aplicados em sequência (dependência de dados dentro da imagem).
                sleep(T_GRAYSCALE); task.grayscaleDone = true;
                sleep(T_BLUR);      task.blurDone      = true;
                sleep(T_EDGE);      task.edgeDone      = true;

                // Redimensionamentos: independentes entre si, mas dependem dos filtros.
                // Em uma versão mais fina, poderiam ser paralelizados com sub-threads;
                // aqui são sequenciais dentro do worker para manter o design simples.
                sleep(T_THUMB); task.thumbDone = true;
                sleep(T_WEB);   task.webDone   = true;
                sleep(T_PRINT); task.printDone = true;

                System.out.printf("[Worker-%s] Imagem %d processada%n",
                        Thread.currentThread().getName(), task.id);

                try {
                    saveQueue.put(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    // ── Estágio 3: Consumidor (salvar + metadados) ───────────────────────────

    /**
     * Consumidor: retira imagens processadas da saveQueue, simula gravação em
     * disco e geração de metadados.
     * Encerra ao receber o único poison pill enviado pelo monitor de workers.
     */
    static class ImageSaver implements Runnable {
        private final BlockingQueue<ImageTask> saveQueue;

        ImageSaver(BlockingQueue<ImageTask> saveQueue) {
            this.saveQueue = saveQueue;
        }

        @Override
        public void run() {
            while (true) {
                ImageTask task;
                try {
                    task = saveQueue.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                if (task.isPoison()) return; // todos os workers terminaram

                sleep(T_SAVE);     // I/O: escrita em disco
                sleep(T_METADATA); // CPU leve: gerar JSON/XML de metadados

                int done = imagesProcessed.incrementAndGet();
                totalSavedBytes.addAndGet(1024 * 1024); // simula ~1 MB por imagem
                System.out.printf("[Saver] Imagem %d salva. Total concluídas: %d%n", task.id, done);
            }
        }
    }

    // ── Monitor: aguarda workers e injeta poison para o Saver ───────────────

    /**
     * Após todos os workers terminarem (join), injeta um poison pill na saveQueue
     * para encerrar o Saver. Roda em thread separada para não bloquear a main.
     */
    static class WorkerMonitor implements Runnable {
        private final Thread[] workers;
        private final BlockingQueue<ImageTask> saveQueue;

        WorkerMonitor(Thread[] workers, BlockingQueue<ImageTask> saveQueue) {
            this.workers   = workers;
            this.saveQueue = saveQueue;
        }

        @Override
        public void run() {
            for (Thread w : workers) {
                try { w.join(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // Todos os workers terminaram: sinaliza o Saver.
            try { saveQueue.put(POISON); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ── Utilitário ───────────────────────────────────────────────────────────

    static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Main ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws InterruptedException {
        int totalImages = 10; // padrão
        int numWorkers  = Runtime.getRuntime().availableProcessors();

        if (args.length >= 1) totalImages = Integer.parseInt(args[0]);
        if (args.length >= 2) numWorkers  = Integer.parseInt(args[1]);

        System.out.printf("Pipeline: %d imagens, %d workers%n%n", totalImages, numWorkers);

        // Filas que conectam os estágios do pipeline.
        BlockingQueue<ImageTask> loadQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<ImageTask> saveQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

        // Estágio 1: produtor único (I/O-bound — não precisa de pool).
        Thread loader = new Thread(new ImageLoader(totalImages, numWorkers, loadQueue), "Loader");

        // Estágio 2: pool de workers (CPU-bound).
        Thread[] workers = new Thread[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            workers[i] = new Thread(new ImageProcessor(loadQueue, saveQueue), "Worker-" + i);
        }

        // Estágio 3: consumidor único (I/O-bound).
        Thread saver = new Thread(new ImageSaver(saveQueue), "Saver");

        // Monitor que notifica o Saver quando todos os workers acabarem.
        Thread monitor = new Thread(new WorkerMonitor(workers, saveQueue), "Monitor");

        // Inicia na ordem correta: consumidor antes do produtor evita perda de itens.
        saver.start();
        for (Thread w : workers) w.start();
        monitor.start();
        loader.start();

        // Aguarda pipeline completo.
        loader.join();
        monitor.join();
        saver.join();

        System.out.printf("%nPipeline concluído: %d imagens, %.1f MB salvos%n",
                imagesProcessed.get(), totalSavedBytes.get() / (1024.0 * 1024.0));
    }
}
