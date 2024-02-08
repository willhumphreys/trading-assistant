package uk.co.threebugs.darwinexclient.metatrader

import uk.co.threebugs.darwinexclient.utils.*
import java.nio.file.*

/*Helpers class

This class includes helper functions for printing, formatting and file operations. 

*/
object Helpers {
    /*Prints to console output. 
	
	Args:
		obj (Object): Object to print.
	*/
    /**
     *
     */
    /*Tries to sleep for a given time period.
	
	Args:
		millis (int): milliseconds to sleep. 
	
	*/
    fun sleep(millis: Int) {
        try {
            Thread.sleep(millis.toLong())
        } catch (e: Exception) {
        }
    }

    /*Tries to read a file.

    Args:
        filePath (String): file path of the file.

    */
    fun tryReadFile(filePath: Path): String {
        val f = filePath.toFile()
        return if (!f.exists()) {
            //logger.info("ERROR: file does not exist: " + filePath);
            ""
        } else try {
            Files.readString(filePath) // , StandardCharsets.US_ASCII
        } catch (e: Exception) {
            logger.error("Unable to read file: $filePath")
            ""
        }
    }

    /*Tries to write to a file. 
	
	Args:
		filePath (String): file path of the file.
		text (String): text to write. 
	
	*/
    fun tryWriteToFile(filePath: Path?, text: String): Boolean {
        return try {
            Files.write(filePath, text.toByteArray())
            true
        } catch (e: Exception) {
            logger.error("Unable to write file: $filePath")
            false
        }
    }

    /*Tries to delete a file.

    Args:
        filePath (String): file path of the file.

    */
    fun tryDeleteFile(filePath: Path?): Boolean {
        return try {
            Files.deleteIfExists(filePath)
            true
        } catch (e: Exception) {
            // e.printStackTrace();
            false
        }
    }
}
