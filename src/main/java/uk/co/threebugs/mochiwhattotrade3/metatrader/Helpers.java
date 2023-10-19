package uk.co.threebugs.mochiwhattotrade3.metatrader;

import java.nio.file.Files;
import java.nio.file.Path;


/*Helpers class

This class includes helper functions for printing, formatting and file operations. 

*/

public class Helpers {


	/*Prints to console output. 
	
	Args:
		obj (Object): Object to print.
	*/

    /**
     *
     */ /*Tries to sleep for a given time period.
	
	Args:
		millis (int): milliseconds to sleep. 
	
	*/
    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
        }
    }


    /*Tries to read a file.

    Args:
        filePath (String): file path of the file.

    */
    public static String tryReadFile(Path filePath) {

        final var f = filePath.toFile();
        if (!f.exists()) {
            //logger.info("ERROR: file does not exist: " + filePath);
            return "";
        }

        try {
            return Files.readString(filePath);  // , StandardCharsets.US_ASCII
        } catch (Exception e) {
            // e.printStackTrace();
            return "";
        }
    }


    /*Tries to write to a file. 
	
	Args:
		filePath (String): file path of the file.
		text (String): text to write. 
	
	*/
    public static boolean tryWriteToFile(Path filePath, String text) {
        try {
            Files.write(filePath, text.getBytes());
            return true;
        } catch (Exception e) {
            // e.printStackTrace();
            return false;
        }
    }


    /*Tries to delete a file.

    Args:
        filePath (String): file path of the file.

    */
    public static boolean tryDeleteFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
            return true;
        } catch (Exception e) {
            // e.printStackTrace();
            return false;
        }
    }
}
