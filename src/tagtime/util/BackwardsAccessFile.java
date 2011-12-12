/*
 * Copyright 2011 Joseph Cloutier, Daniel Reeves, Bethany Soule
 * 
 * This file is part of TagTime.
 * 
 * TagTime is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * TagTime is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TagTime. If not, see <http://www.gnu.org/licenses/>.
 */

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
	 * @param requiredCharacters See
	 *            {@link BackwardsAccessFile#seekLineStart(String)} for a
	 *            description of this parameter.
	 * @throws IOException if an I/O error occurs.
	 */
	public void seekLastLine(String requiredCharacters) throws IOException {
		seek(length());
		seekLineStart(requiredCharacters);
	}
	
	/**
	 * Finds the first line before this one containing at least one of
	 * the given characters, then sets this file's read index to the
	 * position before the first character of that line.
	 * @param requiredCharacters A set of characters to look for before
	 *            returning - at least one of these must be found on a
	 *            line for that line to be considered valid (for the
	 *            current line, one of these must be found
	 *            <em>before</em> the current read index). If this is
	 *            null, the function will stop at the first line it
	 *            finds. If this is empty, the function will stop at the
	 *            first line with any non-newline characters.
	 * @throws IOException If an I/O error occurs.
	 */
	public void seekLineStart(String requiredCharacters) throws IOException {
		//using a buffer because directly iterating backwards would
		//probably be inefficient
		int bufferSize = 64;
		byte[] buffer = new byte[bufferSize];
		
		char character;
		
		//if there are no required characters, none need to be found
		boolean requiredCharacterFound = requiredCharacters == null;
		
		//iterate backwards to find the final occurrence of the string
		long index;
		for(index = getFilePointer() - bufferSize; index > -bufferSize; index -= bufferSize) {
			if(index >= 0) {
				seek(index);
				readFully(buffer);
			} else {
				//the first few bytes will most likely be enough to
				//entirely fill the buffer, so handle them separately
				seek(0);
				
				//hack: because this is the final iteration, it is safe
				//to modify bufferSize
				//(index is negative, so this actually reduces it)
				bufferSize += index;
				readFully(buffer, 0, bufferSize);
			}
			
			for(int offset = bufferSize - 1; offset >= 0; offset--) {
				character = (char) buffer[offset];
				
				//if the character is a new line
				if(character == '\n' || character == '\r') {
					if(requiredCharacterFound) {
						seek(index + offset + 1);
						return;
					}
				}

				else if(!requiredCharacterFound && requiredCharacters != null) {
					if(requiredCharacters.length() == 0
								|| requiredCharacters.indexOf(character) != -1) {
						requiredCharacterFound = true;
					}
				}
			}
		}
		
		//if no appropriate lines were found, go to the start of the file
		seek(0);
	}
	
	/**
	 * Sets this file's read/write index to the position before the first
	 * character of the line containing the given index.
	 * @throws IOException If pos is less than 0 or an I/O error occurs.
	 */
	public void seekLineStart(long pos) throws IOException {
		seek(pos);
		seekLineStart(null);
	}
	
	/**
	 * Returns the final line of the file containing at least one of the
	 * given characters.
	 * @param requiredCharacters See
	 *            {@link BackwardsAccessFile#seekLineStart(String)} for a
	 *            description of this parameter.
	 * @return The line that was found.
	 * @throws IOException if an I/O error occurs.
	 */
	public String readLastLine(String requiredCharacters) throws IOException {
		seekLastLine(requiredCharacters);
		
		return readLine();
	}
	
	/**
	 * Finds the start of the current line, then reads the entire thing.
	 * @return The line that was found.
	 * @throws IOException if an I/O error occurs.
	 */
	public String readFullLine() throws IOException {
		seekLineStart(null);
		
		return readLine();
	}
	
	/**
	 * Reads the last non-empty line before the current line.
	 * @param requiredCharacters See
	 *            {@link BackwardsAccessFile#seekLineStart(String)} for a
	 *            description of this parameter.
	 * @return The entire line before the line that the pointer is on.
	 * @throws IOException if an I/O error occurs.
	 */
	public String readPreviousLine(String requiredCharacters) throws IOException {
		seekPreviousLine(requiredCharacters);
		return readLine();
	}
	
	/**
	 * Moves the pointer to the start of the last non-empty line before
	 * the current line.
	 * @param requiredCharacters See
	 *            {@link BackwardsAccessFile#seekLineStart(String)} for a
	 *            description of this parameter.
	 * @throws IOException if an I/O error occurs.
	 */
	public void seekPreviousLine(String requiredCharacters) throws IOException {
		seekLineStart(null);
		
		if(getFilePointer() > 0) {
			if(requiredCharacters == null) {
				seek(getFilePointer() - 1);
			}
			seekLineStart(requiredCharacters);
		}
	}
}
