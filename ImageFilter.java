import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageFilter {
    public static void main(String[] args) {

        File file = new File("assets/lion.jpg");
        BufferedImage image = null;
        int threshold = 70;

        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }

        if (image != null) {
            image = toGrayScale(image);
            image = pixelate(image, 3);
            image = resize(image, 0.5);

            BufferedImage image1 = complexGaussianBlur(image, 1);
            BufferedImage image2 = complexGaussianBlur(image, 10);

            image = subtractImages(image1, image2);
            image = detectEdge(image, threshold);
            display(image);
        }
    }

    /**
     * Display an image
     *
     * @param image BufferedImage to process
     */
    public static void display(BufferedImage image) {
        // Scale up the image for easier viewing
        int scaleFactor = 1; // Increase size by a factor of 10 (adjustable)
        int newWidth = image.getWidth() * scaleFactor;
        int newHeight = image.getHeight() * scaleFactor;

        Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

        // Create a frame and label
        JFrame frame = new JFrame("Image Viewer");
        JLabel label = new JLabel();

        // Set the scaled image to the label
        label.setIcon(new ImageIcon(scaledImage));

        // Configure frame properties
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();

        // Center and display the frame
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setVisible(true);
    }

    /**
     * Turn image to grayscale
     *
     * @param image BufferedImage to process
     * @return BufferedImage
     */
    public static BufferedImage toGrayScale(BufferedImage image) {
        System.out.println("Converting to gray scale...");
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grayImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return grayImage;
    }

    /**
     * Pixelate a grayscale image
     *
     * @param image BufferedImage to process
     * @param kernel Pixelate kernel size NxN
     * @return BufferedImage
     */
    public static BufferedImage pixelate(BufferedImage image, int kernel) {
        System.out.println("Pixelating image...");
        BufferedImage pixelateImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        int pixel = 0, p = 0;
        for (int x = 0; x < image.getWidth() - kernel; x += kernel) {
            for (int y = 0; y < image.getHeight() - kernel; y += kernel) {
                for (int i = 0; i < kernel; i++) {
                    for (int j = 0; j < kernel; j++) {
                        pixel += (image.getRGB(x + i, y + j) & 0xff);
                    }
                }

                pixel = (int) pixel / kernel / kernel;
                p = (255<<24) | (pixel<<16) | (pixel<<8) | pixel;
                for (int i = 0; i < kernel; i++) {
                    for (int j = 0; j < kernel; j++) {
                        pixelateImage.setRGB(x + i, y + j, p);
                    }
                }
                pixel = 0;
            }
        }

        return pixelateImage;
    }

    /**
     * Resize image
     *
     * @param image BufferedImage to process
     * @param percent New size percentage
     * @return BufferedImage
     */
    public static BufferedImage resize(BufferedImage image, double percent) {
        System.out.println("Resizing image...");
        int newWidth = (int) (image.getWidth() * percent);
        int newHeight = (int) (image.getHeight() * percent);
        BufferedImage resizeImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
        AffineTransform at = new AffineTransform();
        at.scale(percent, percent);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        return scaleOp.filter(image, resizeImage);
    }

    /**
     * Blur image with 3x3 kernel
     *
     * @param image BufferedImage to process
     * @return BufferedImage
     */
    public static BufferedImage gaussianBlur(BufferedImage image) {
        System.out.println("GaussianBlur...");
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();
        BufferedImage blurImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
        int pixel = 0;
        int[][] offsets = {
            {0, 0, 4},  // center
            {0, -1, 2}, {0, 1, 2},   // top, bottom
            {-1, 0, 2}, {1, 0, 2},   // left, right
            {-1, -1, 1}, {-1, 1, 1}, // top-left, bottom-left
            {1, -1, 1}, {1, 1, 1}    // top-right, bottom-right
        };

        for (int y = 0; y < blurImage.getHeight(); y++) {
            for (int x = 0; x < blurImage.getWidth(); x++) {
                pixel = applyKernel(image, offsets, imgWidth, imgHeight, x, y);
                int p = (255<<24) | (pixel<<16) | (pixel<<8) | pixel;
                blurImage.setRGB(x, y, p);
            }
        }

        return blurImage;
    }

    /**
     * Blur image with 3x3 kernel
     *
     * @param image BufferedImage to process
     * @return BufferedImage
     */
    public static BufferedImage massGaussianBlur(BufferedImage image) {
        System.out.println("MassGaussianBlur...");
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();
        BufferedImage blurImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY);
        int pixel = 0;
        int[][] offsets = {
                {0, 0, 10},               // center
                {0, -1, 6}, {0, 1, 6},    // near top, bottom
                {0, -2, 4}, {0, 2, 4},    // far top, bottom
                {-1, 0, 6}, {1, 0, 6},    // near left, right
                {-2, 0, 4}, {2, 0, 4},    // far left, right
                {-1, -1, 4}, {-1, 1, 4},  // near top-left, bottom-left
                {-2, -2, 1}, {-2, 2, 1},  // far top-left, bottom-left
                {1, -1, 4}, {1, 1, 4},    // near top-right, bottom-right
                {2, -2, 1}, {2, 2, 1},    // near top-right, bottom-right
                {-1, -2, 2}, {-1, 2, 2},  // the rest
                {1, -2, 2}, {1, 2, 2},
                {-2, -1, 2}, {-2, 1, 2},
                {2, -1, 2}, {2, 1, 2}
        };

        for (int y = 0; y < blurImage.getHeight(); y++) {
            for (int x = 0; x < blurImage.getWidth(); x++) {
                pixel = applyKernel(image, offsets, imgWidth, imgHeight, x, y);
                int p = (255<<24) | (pixel<<16) | (pixel<<8) | pixel;
                blurImage.setRGB(x, y, p);
            }
        }

        return blurImage;
    }

    /**
     * Apply a 3x3 kernel to a specific pixel of an image.
     *
     * @param image  BufferedImage to process
     * @param offsets Kernel offset map
     * @param width  Width of the image
     * @param height Height of the image
     * @param x      X-coordinate of the pixel
     * @param y      Y-coordinate of the pixel
     * @return int   Resulting averaged value
     */
    private static int applyKernel(BufferedImage image, int[][] offsets, int width, int height, int x, int y) {
        int total = 0;
        int weight = 0;

        // Loop through kernel offsets
        for (int[] offset : offsets) {
            int dx = x + offset[0];
            int dy = y + offset[1];
            int w = offset[2];

            if (dx >= 0 && dx < width && dy >= 0 && dy < height) {
                total += (image.getRGB(dx, dy) & 0xff) * w;
                weight += w;
            }
        }

        return total / weight;
    }

    public static BufferedImage detectEdge(BufferedImage image, int threshold) {
        System.out.println("Detecting edges...");
        int w = image.getWidth(), h = image.getHeight(), pixel = 0;
        BufferedImage edgeImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        int[][] vert = new int[w][h];
        int[][] horiz = new int[w][h];
        int[][] edgeWeight = new int[w][h];

        int[][] vertOffsets = {
            {-1, -1, 1}, {-1, 0, 2}, {-1, 1, 1},
            {1, -1, -1}, {1, 0, -2}, {1, 1, -1},
        };
        int[][] horizOffsets = {
            {-1, -1, 1}, {0, -1, 2}, {1, -1, 1},
            {-1, 1, -1}, {0, 1, -2}, {1, 1, -1}
        };

        for (int x = 1; x < w-1; x++) {
            for (int y = 1; y < h-1; y++) {
                for (int[] offset : vertOffsets) {
                    int dx = x + offset[0];
                    int dy = y + offset[1];
                    int weight = offset[2];

                    vert[x][y] += (image.getRGB(dx, dy) & 0xff) * weight;
                }

                for (int[] offset : horizOffsets) {
                    int dx = x + offset[0];
                    int dy = y + offset[1];
                    int weight = offset[2];

                    horiz[x][y] += (image.getRGB(dx, dy) & 0xff) * weight;
                }

                edgeWeight[x][y] = (int) Math.sqrt(vert[x][y] * vert[x][y] + horiz[x][y] * horiz[x][y]);
                if (edgeWeight[x][y] > threshold) {
                    pixel = (255<<24) | (255<<16) | (255<<8) | 255;
                } else {
                    pixel = (255<<24) | (0<<16) | (0<<8) | 0;
                }
                edgeImage.setRGB(x, y, pixel);
            }
        }

        return edgeImage;
    }

    public static BufferedImage differenceOfGaussians(BufferedImage image1, BufferedImage image2) {
        System.out.println("MassGaussianBlur...");
        BufferedImage dogImage = new BufferedImage(image1.getWidth(), image1.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        System.out.println(image1.getRGB(50, 50) & 0xff);
        System.out.println(image2.getRGB(50, 50) & 0xff);
        for (int x = 0; x < image1.getWidth(); x++) {
            for (int y = 0; y < image1.getHeight(); y++) {
                System.out.println((image1.getRGB(x, y) & 0xff) + " " + (image2.getRGB(x, y) & 0xff));
                int pixel = (int) Math.abs((image1.getRGB(x, y) & 0xff) - (image2.getRGB(x, y) & 0xff));
                int p = (255<<24) | (pixel<<16) | (pixel<<8) | pixel;
                dogImage.setRGB(x, y, p);
            }
        }

        return dogImage;
    }

    /**
     * Brighten image
     *
     * @param image BufferedImage to process
     * @param percentage Brightness percentage
     * @return BufferedImage
     */
    public static BufferedImage brighten(BufferedImage image, int percentage) {
        System.out.println("Brightening...");
        BufferedImage brightenImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int pixel = 0;
        int amount = (int)((percentage * 255) / 100);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int color = image.getRGB(x, y);
                int r = ((color >> 16) & 0xff) + amount;
                int g = ((color >> 8) & 0xff) + amount;
                int b = ((color) & 0xff) + amount;
                if (r > 255) r = 255;
                if (g > 255) g = 255;
                if (b > 255) b = 255;
                pixel = (255<<24) | (r << 16) | (g << 8) | b;
                brightenImage.setRGB(x, y, pixel);
            }
        }
        return brightenImage;
    }

    /**
     * @param image
     * @param radius
     * @return
     */
    public static BufferedImage complexGaussianBlur(BufferedImage image, int radius) {
        double sigma = radius / 2.0;
        int kernelWidth = (radius * 2) + 1;

        BufferedImage blurImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        double[][] kernel = buildKernel(radius, kernelWidth, sigma);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                double[] colors = applyGaussianKernel(image, x, y, kernel, radius);
                int r = (int) colors[0];
                int g = (int) colors[1];
                int b = (int) colors[2];
                int col = (255<<24) | (r << 16) | (g << 8) | b;
                blurImage.setRGB(x, y, col);
            }
        }
        return blurImage;
    }

    /**
     *
     * @param radius
     * @param kernelWidth
     * @param sigma
     * @return
     */
    private static double[][] buildKernel(int radius, int kernelWidth, double sigma) {
        double[][] kernel = new double[kernelWidth][kernelWidth];
        double sum = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                double exponentNumerator = (double) (- (x * x + y * y));
                double exponentDenominator = (double) (2 * sigma * sigma);

                double eExpression = Math.exp(exponentNumerator / exponentDenominator);
                double kernelValue = (eExpression / (2 * Math.PI * sigma * sigma));

                kernel[x + radius][y + radius] = kernelValue;
                sum += kernelValue;
            }
        }

        for (int x = 0; x < kernelWidth; x++) {
            for (int y = 0; y < kernelWidth; y++) {
                kernel[x][y] /= sum;
            }
        }

        return kernel;
    }

    /**
     *
     * @param image
     * @param x
     * @param y
     * @param kernel
     * @param radius
     * @return
     */
    private static double[] applyGaussianKernel(BufferedImage image, int x, int y, double[][] kernel, int radius) {

        double[] colors = new double[3];
        double red = 0.0, green = 0.0, blue = 0.0;

        for (int kernelX = -radius; kernelX <= radius; kernelX++) {
            for (int kernelY = -radius; kernelY <= radius; kernelY++) {
                int neighborX = x + kernelX;
                int neighborY = y + kernelY;

                if (neighborX >= 0 && neighborX < image.getWidth() && neighborY >= 0 && neighborY < image.getHeight()) {
                    double kernelValue = kernel[kernelX + radius][kernelY + radius];
                    int color = image.getRGB(neighborX, neighborY);
                    red += ((color >> 16) & 0xff) * kernelValue;
                    green += ((color >> 8) & 0xff) * kernelValue;
                    blue += ((color) & 0xff) * kernelValue;
                }
            }
        }

        colors[0] = red;
        colors[1] = green;
        colors[2] = blue;
        return colors;
    }

    /**
     *
     * @param image1
     * @param image2
     * @return
     */
    public static BufferedImage subtractImages(BufferedImage image1, BufferedImage image2) {
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image1.getHeight(), image2.getHeight());
        BufferedImage subtractImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb1 = image1.getRGB(x, y);
                int rgb2 = image2.getRGB(x, y);

                int r1 = (rgb1 >> 16) & 0xFF;
                int g1 = (rgb1 >> 8) & 0xFF;
                int b1 = rgb1 & 0xFF;

                int r2 = (rgb2 >> 16) & 0xFF;
                int g2 = (rgb2 >> 8) & 0xFF;
                int b2 = rgb2 & 0xFF;

                int r = Math.abs(r1 - r2);
                int g = Math.abs(g1 - g2);
                int b = Math.abs(b1 - b2);

                // Combine new RGB values
                int newRgb = (255 << 24) | (r << 16) | (g << 8) | b;

                // Set the pixel in the result image
                subtractImage.setRGB(x, y, newRgb);
            }
        }

        return subtractImage;
    }
}
