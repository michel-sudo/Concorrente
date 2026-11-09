import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class provides functionality to apply a mean filter to an image.
 * The mean filter is used to smooth images by averaging the pixel values
 * in a neighborhood defined by a kernel size.
 * 
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * ImageMeanFilter.applyMeanFilter("input.jpg", "output.jpg", 3);
 * }
 * </pre>
 * 
 * <p>Supported image formats: JPG, PNG</p>
 * 
 * <p>Author: temmanuel@comptuacao.ufcg.edu.br</p>
 */
public class ImageMeanFilter {
    
    /**
     * Applies mean filter to an image
     * 
     * @param inputPath  Path to input image
     * @param outputPath Path to output image 
     * @param kernelSize Size of mean kernel
     * @param numThreads Number of threads
     * @throws IOException If there is an error reading/writing
     */
    public static void applyMeanFilter(String inputPath, String outputPath, int kernelSize, int numThreads)
            throws IOException, InterruptedException {
        // Load image
        BufferedImage originalImage = ImageIO.read(new File(inputPath));
        
        // Create result image
        BufferedImage filteredImage = new BufferedImage(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            BufferedImage.TYPE_INT_RGB
        );
        
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // Cada thread processa uma faixa de linhas e guarda seus próprios contadores
        // (assim não há condição de corrida nem necessidade de lock).
        Thread[] threads = new Thread[numThreads];
        long[] changed = new long[numThreads];
        long[] unchanged = new long[numThreads];
        int rowsPerThread = (height + numThreads - 1) / numThreads;

        for (int t = 0; t < numThreads; t++) {
            final int id = t;
            final int startY = t * rowsPerThread;
            final int endY = Math.min(height, startY + rowsPerThread);
            threads[t] = new Thread(() -> {
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        int[] avgColor = calculateNeighborhoodAverage(originalImage, x, y, kernelSize);
                        int newRgb = (avgColor[0] << 16) | (avgColor[1] << 8) | avgColor[2];
                        int oldRgb = originalImage.getRGB(x, y) & 0xFFFFFF;

                        // Pixels diferentes por thread: escrita segura
                        filteredImage.setRGB(x, y, newRgb);

                        if (newRgb != oldRgb) changed[id]++;
                        else unchanged[id]++;
                    }
                }
            });
            threads[t].start();
        }

        long totalChanged = 0, totalUnchanged = 0;
        for (int t = 0; t < numThreads; t++) {
            threads[t].join();
            totalChanged += changed[t];
            totalUnchanged += unchanged[t];
        }

        System.out.println("Pixels alterados: " + totalChanged);
        System.out.println("Pixels iguais: " + totalUnchanged);
        
        // Save filtered image
        ImageIO.write(filteredImage, "jpg", new File(outputPath));
    }
    
    /**
     * Calculates average colors in a pixel's neighborhood
     * 
     * @param image      Source image
     * @param centerX    X coordinate of center pixel
     * @param centerY    Y coordinate of center pixel
     * @param kernelSize Kernel size
     * @return Array with R, G, B averages
     */
    private static int[] calculateNeighborhoodAverage(BufferedImage image, int centerX, int centerY, int kernelSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pad = kernelSize / 2;
        
        // Arrays for color sums
        long redSum = 0, greenSum = 0, blueSum = 0;
        int pixelCount = 0;
        
        // Process neighborhood
        for (int dy = -pad; dy <= pad; dy++) {
            for (int dx = -pad; dx <= pad; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                
                // Check image bounds
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    // Get pixel color
                    int rgb = image.getRGB(x, y);
                    
                    // Extract color components
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;
                    
                    // Sum colors
                    redSum += red;
                    greenSum += green;
                    blueSum += blue;
                    pixelCount++;
                }
            }
        }
        
        // Calculate average
        return new int[] {
            (int)(redSum / pixelCount),
            (int)(greenSum / pixelCount),
            (int)(blueSum / pixelCount)
        };
    }
    
    /**
     * Main method for demonstration
     * 
     * Usage: java ImageMeanFilter <input_file>
     * 
     * Arguments:
     *   input_file - Path to the input image file to be processed
     *                Supported formats: JPG, PNG
     * 
     * Example:
     *   java ImageMeanFilter input.jpg
     * 
     * The program will generate a filtered output image named "filtered_output.jpg"
     * using a 7x7 mean filter kernel
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ImageMeanFilter <input_file> <num_threads>");
            System.exit(1);
        }

        String inputFile = args[0];
        int numThreads = Integer.parseInt(args[1]);
        if (numThreads < 2) {
            System.err.println("O número de threads deve ser pelo menos 2.");
            System.exit(1);
        }
        try {
            applyMeanFilter(inputFile, "filtered_output.jpg", 7, numThreads);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error processing image: " + e.getMessage());
        }
    }
}