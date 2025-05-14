package utils

import java.awt.AlphaComposite
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object ImageProcessor {
    
    /**
     * Processes the image by resizing it to the target dimensions while maintaining aspect ratio.
     * If the aspect ratio doesn't match, the image is center-cropped.
     * 
     * @param imageBytes The original image as a ByteArray
     * @param targetWidth The target width for the resized image
     * @param targetHeight The target height for the resized image
     * @param format The output format (jpg or png)
     * @return The processed image as a ByteArray
     */
    fun process(
        imageBytes: ByteArray, 
        targetWidth: Int = 400, 
        targetHeight: Int = 400,
        format: String = "jpg"
    ): ByteArray {
        // Read the original image
        val inputStream = ByteArrayInputStream(imageBytes)
        val originalImage = ImageIO.read(inputStream)
        
        // Create a new buffered image with the target dimensions
        val resizedImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = resizedImage.createGraphics()
        
        // Set rendering hints for better quality
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        // Calculate dimensions to maintain aspect ratio
        val originalWidth = originalImage.width
        val originalHeight = originalImage.height
        
        var x = 0
        var y = 0
        var width = originalWidth
        var height = originalHeight
        
        // Calculate scaling and position to center-crop if needed
        val originalAspect = originalWidth.toFloat() / originalHeight
        val targetAspect = targetWidth.toFloat() / targetHeight
        
        if (originalAspect > targetAspect) {
            // Original is wider, crop width
            width = (originalHeight * targetAspect).toInt()
            x = (originalWidth - width) / 2
        } else if (originalAspect < targetAspect) {
            // Original is taller, crop height
            height = (originalWidth / targetAspect).toInt()
            y = (originalHeight - height) / 2
        }
        
        // Draw the image with the calculated dimensions
        graphics.drawImage(originalImage, 0, 0, targetWidth, targetHeight, x, y, x + width, y + height, null)
        graphics.dispose()
        
        // Write the resized image to a byte array
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(resizedImage, format, outputStream)
        
        return outputStream.toByteArray()
    }

    /**
     * Validates if the provided bytes represent a valid image in the supported formats
     */
    fun isValidImage(bytes: ByteArray): Boolean {
        return try {
            val inputStream = ByteArrayInputStream(bytes)
            val image = ImageIO.read(inputStream)
            image != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets the format of the image (jpg or png)
     */
    fun getImageFormat(bytes: ByteArray): String? {
        try {
            val inputStream = ByteArrayInputStream(bytes)
            val readers = ImageIO.getImageReadersBySuffix("*")
            
            while (readers.hasNext()) {
                val reader = readers.next()
                try {
                    val stream = ImageIO.createImageInputStream(inputStream)
                    reader.input = stream
                    return reader.formatName.lowercase()
                } catch (e: Exception) {
                    // Try next reader
                    inputStream.reset()
                } finally {
                    reader.dispose()
                }
            }
        } catch (e: Exception) {
            // Do nothing
        }
        
        return null
    }
} 