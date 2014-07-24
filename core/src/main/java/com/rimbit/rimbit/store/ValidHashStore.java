/**
 * Copyright 2014 Rimbit Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rimbit.rimbit.store;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.omg.CORBA_2_3.portable.OutputStream;
import org.spongycastle.util.encoders.Hex;

import com.google.common.base.Objects;
import com.google.common.io.Files;
import com.google.common.primitives.Bytes;
import com.rimbit.rimbit.core.AbstractBlockChain;
import com.rimbit.rimbit.core.Sha256Hash;
import com.rimbit.rimbit.core.StoredBlock;
import com.rimbit.rimbit.core.Utils;

public class ValidHashStore {
	

	private RandomAccessFile file;
	private List<byte[]> validHashes = new ArrayList<byte[]>();
	private static URL VALID_HASHES_URL;
	
	static {
		try {
			VALID_HASHES_URL = new URL("https://rimbitexplorer.com/chain/Rimbit/q/getvalidhashes");
		} catch (final MalformedURLException x) {
			throw new RuntimeException(x); // cannot happen
		}
	}

	private static String GENESIS_MINI_HASH = "fc6957fc03e96fffe1223a461c454130"; 
	
	public ValidHashStore(File filePath) throws IOException {
		file = new RandomAccessFile(filePath, "rw");
		
		if (file.length() == 0) {
			// Add genesis hash and that is all
			writeHash(Hex.decode(GENESIS_MINI_HASH));
			return;
		}
		
		// Load valid hashes from file.
		byte[] b = new byte[16];
		
		while (file.read(b) == 16) {
			validHashes.add(b); 
			b = new byte[16];
		}
	}
	
	private void writeHash(byte[] hash) throws IOException {
		writeHash(validHashes.size(), hash);
	}
	
	private void writeHash(int index, byte[] hash) throws IOException {
		validHashes.add(index, hash);
		file.seek(index*16);
		file.write(hash);
	}
	
	private boolean isInValidHashes(byte[] cmpHash) {
		for (int x = 0; x < validHashes.size(); x++) 
			if (Arrays.equals(validHashes.get(x), cmpHash))
				return true;
		return false;
	}

	public boolean isValidHash(Sha256Hash hash, AbstractBlockChain blockChain, boolean waitForServer) throws IOException {
		// Get 16 bytes only
		byte[] cmpHash = new byte[16];
	    System.arraycopy(Utils.reverseBytes(hash.getBytes()), 0, cmpHash, 0, 16);
	    
		// First check the existing hashes
		if (isInValidHashes(cmpHash))
			return true;
		
		// Nope. We need to ensure the valid hashes is synchronised with the server
		
		// Create POST data locator
		
		byte[] locator = new byte[3200];
		
        BlockStore store = checkNotNull(blockChain).getBlockStore();
        StoredBlock chainHead = blockChain.getChainHead();

        StoredBlock cursor = chainHead;
        int offset = 0;
        
        for (int i = 100; cursor != null && i > 0; i--, offset += 32) {
        	System.arraycopy(Utils.reverseBytes(cursor.getHeader().getHash().getBytes()), 0, locator, offset, 32);
        	
        	// Ensure this hash is in our valid hashes or we need a complete re-download
        	byte[] smallHash = new byte[16];
    	    System.arraycopy(locator, offset, smallHash, 0, 16);
    	    if (!isInValidHashes(smallHash)) {
    	    	offset = 0;
    	    	break;
    	    }
        	
            try {
                cursor = cursor.getPrev(store);
            } catch (BlockStoreException e) {
                throw new RuntimeException(e);
            }
        }
        
        // Now download hashes from server.
        
        // But if waitForServer is true, first wait a while in case the server hasn't received or processed this block yet.
        // We assume the server is well connected and 30 seconds would therefore be more than enough in most cases.
        if (waitForServer)
        	Utils.sleep(30000);
        
        HttpURLConnection connection = (HttpURLConnection) VALID_HASHES_URL.openConnection();
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(false);
		connection.setConnectTimeout(8000);
		connection.setReadTimeout(8000);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/octet-stream");
		connection.setDoOutput(true);
		java.io.OutputStream os = connection.getOutputStream();
		os.write(locator, 0, offset);
		os.flush();
		os.close();
		connection.connect();
		
		final int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			
			InputStream is = new BufferedInputStream(connection.getInputStream(), 1024);
			
			byte[] b = new byte[16];
			
			// Figure out where we are in blockchain.
			
			is.read(b);
			
			int j = 0;
			for (; ! Arrays.equals(validHashes.get(j), b); j++);
			j++;
			
			// Now write to file and validHashes
			
			for (;is.read(b) == 16; j++) {
				writeHash(j, b);
				b = new byte[16];
			}
			
		} else throw new IOException("Bad response code from server when downloading hashes");
		
		// Lastly check valid hashes again
		return isInValidHashes(cmpHash);
	}
	
	public void close(){
		try {
			file.close();
		} catch (IOException e) {
			// Whatever
		}
	}
	
}
