/*
 * Copyright 2011-2012 Joseph Cloutier
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

package tagtime.random;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * <p>
 * A pseudo-random number generator that produces values by encrypting
 * numeric values. For example, it calculates the first value in the
 * sequence by encrypting the number 0, and the second value by
 * encrypting the number 1. This allows TagTime to iterate over the
 * sequence in either direction.
 * </p>
 * <p>
 * It is unknown whether the numbers generated satisfy statistical tests
 * for randomness.
 * </p>
 */
public class RandomSequenceGenerator {
	private Cipher cipher;
	private Key key;
	
	/**
	 * Creates a new <code>RandomSequenceGenerator</code> with an
	 * automatically-generated key.
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
	public RandomSequenceGenerator() throws InvalidKeyException, NoSuchAlgorithmException,
				NoSuchPaddingException {
		this(null);
	}
	
	/**
	 * Creates a new <code>RandomSequenceGenerator</code> with a
	 * user-provided key. Use this if you want consistent pseudo-random
	 * numbers across multiple executions or generators.
	 * @param keyString The encryption key to use. No safety checking is
	 *            performed, and weak keys will be accepted. If this is
	 *            null or empty, an automatically-generated key will be
	 *            used instead.
	 * @throws NoSuchAlgorithmException If Java has dropped support for
	 *             AES (not likely).
	 * @throws NoSuchPaddingException If Java has dropped support for AES
	 *             (not likely).
	 * @throws InvalidKeyException If the user modifies their RNG_KEY
	 *             setting to a value that cannot be used as an AES key.
	 *             This may or may not be possible (untested).
	 */
	public RandomSequenceGenerator(String keyString) throws NoSuchAlgorithmException,
				NoSuchPaddingException, InvalidKeyException {
		//generate or parse the key
		if(keyString == null || keyString.equals("")) {
			key = KeyGenerator.getInstance("AES").generateKey();
		} else {
			key = new SecretKeySpec(Base64.decodeBase64(keyString), "AES");
		}
		
		//set up the cipher that will be used to generate pseudo-random numbers
		cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, key);
	}
	
	/**
	 * Returns the random value at the given position in the sequence.
	 * @return A pseudo-random value between 0 and 1.
	 */
	public double getValue(long position) {
		ByteBuffer outputBytes = ByteBuffer.allocate(16);
		try {
			cipher.doFinal((ByteBuffer) ByteBuffer.allocate(8).putLong(position).rewind(),
						outputBytes);
		} catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
		
		//convert the bytes to a double between 0 and 1
		return 0.5 + 0.5 * ((double) outputBytes.getLong(0) / Long.MAX_VALUE);
	}
	
	public String getKey() {
		return Base64.encodeBase64String(key.getEncoded());
	}
}
