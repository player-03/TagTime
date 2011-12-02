package tagtime.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * An extension of {@link RandomAccessFile} with methods for finding the
 * last instance of certain types of data.
 */
public class BackwardsAccessFile extends RandomAccessFile {
	public BackwardsAccessFile(String name, String mode) throws FileNotFoundException {
		super(name, mode);
	}
	
	public BackwardsAccessFile(File file, String mode) throws FileNotFoundException {
		super(file, mode);
	}
	
	/**
	 * Sets this file's read/write index to the position before the first
	 * character of the final line.
	 * @param requiredCharacters A set of characters to look for before
	 *            returning - at least one of these must be found on a
	 *            line for that line to be considered valid. If this is
	 *            null or empty, the function will stop at the first line
	 *            it finds.
	 * @throws IOException if an I/O error occurs.
	 */
	public void seekLastLine(String requiredCharacters) throws IOException {
		//using a buffer because directly iterating backwards would
		//probably be inefficient
		int bufferSize = 64;
		byte[] buffer = new byte[bufferSize];
		
		char character;
		
		boolean requiredCharacterFound = requiredCharacters == null ||
					requiredCharacters.length() == 0;
		
		//iterate backwards to find the final occurrence of the string
		for(long index = length() - bufferSize; index >= 0; index -= bufferSize) {
			seek(index);
			readFully(buffer);
			
			for(int offset = bufferSize - 1; offset >= 0; offset -= 1) {
				//read two bytes to get the current character
				character = (char) buffer[offset];
				
				//if the character is a new line
				if(character == '\n' || character == '\r') {
					if(requiredCharacterFound) {
						seek(index + offset + 1);
						return;
					}
				}
				
				if(requiredCharacters != null &&
							requiredCharacters.indexOf(character) != -1) {
					requiredCharacterFound = true;
				}
			}
		}
	}
	
	/**
	 * Returns the final line of the file containing at least one of the
	 * given characters.
	 * @param requiredCharacters A set of characters to look for before
	 *            returning - at least one of these must be found on a
	 *            line for that line to be considered valid. If this is
	 *            null or empty, the function will return the final line
	 *            in the file (most likely an empty line).
	 * @return The line that was found.
	 * @throws IOException if an I/O error occurs.
	 */
	public String readLastLine(String requiredCharacters) throws IOException {
		seekLastLine(requiredCharacters);
		
		return readLine();
	}
}
