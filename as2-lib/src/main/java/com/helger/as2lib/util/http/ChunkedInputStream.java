package com.helger.as2lib.util.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Stream to read a chuncked body stream. Input stream should be at the beginning of a chunk, i.e. at the body beginning (after the end of headers marker). THe resulting stream reads the data through the chunks.
 *
 * @author Ziv Harpaz
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ChunkedInputStream extends FilterInputStream {
	private static final Logger s_aLogger = LoggerFactory.getLogger (ChunkedInputStream.class);
	/* Number of bytes left in current chunk

	 */
	int nLeft =0;
	final InputStream aIS;
	boolean afterFirstChunk = false;

	public ChunkedInputStream(@Nonnull InputStream is) {
		super(is);
		aIS = is;
	}
	@Override
	public final int read() throws IOException {
		if (nLeft < 0)
			return -1;
		if (nLeft == 0) {
			if (afterFirstChunk) {
				//read the CRLF after chunk data
				HTTPHelper.readTillNexLine(aIS);
			} else {
				afterFirstChunk = true;
			}
			nLeft = HTTPHelper.readChunkLen(aIS);
			s_aLogger.debug("Read chunk size:{}", nLeft);
			//check for end of data
			if (nLeft <= 0) {
				// No more chunks means EOF
				nLeft = -1; //mark end of stream
				return -1;
			}
		}
		nLeft--;
		return super.read();
	}

	@Override
	public final int read(@Nonnull byte[] b, int nOffset, int nLength) throws IOException {
		if (nLeft < 0)
			return -1;
		int readCount = 0;
		while (nLength > readCount){
			if (nLeft == 0) {
				if (afterFirstChunk) {
					//read the CRLF after chunk data
					HTTPHelper.readTillNexLine(aIS);
				} else {
					afterFirstChunk = true;
				}
				nLeft = HTTPHelper.readChunkLen(aIS);
				s_aLogger.debug("Read chunk size:{}", nLeft);
				//check for end of data
				if (nLeft <= 0) {
					// No more chunks means EOF
					nLeft = -1; //mark end of stream
					return readCount > 0 ? readCount : -1;
				}
			}
			int ret = super.read(b, nOffset, Math.min(nLength-readCount, nLeft));
			nOffset   += ret;
			nLeft     -= ret;
			readCount += ret;
		}
		return readCount;
	}


}
