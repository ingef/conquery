package com.bakdata.conquery.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;

import com.bakdata.conquery.util.io.ConqueryFileUtil;

public class HCFile implements Closeable {
	
	private RandomAccessFile raf;
	private File tmpFile;
	private BufferedOutputStream tmpOut;
	
	public HCFile(File f, boolean write) throws IOException {
		this.raf = new RandomAccessFile(f, write?"rw":"r");
		
		//if write we reserve space for the content starting position
		if(write) {
			raf.writeLong(-1);
			tmpFile = ConqueryFileUtil.createTempFile("hcfile", "tmp");
			tmpOut = new BufferedOutputStream(new FileOutputStream(tmpFile));
		}
	}

	public OutputStream writeHeader() throws IOException {
		return new BufferedOutputStream(
			new CloseShieldOutputStream(
				new FileOutputStream(raf.getFD())
			)
		);
	}
	
	public InputStream readHeader() throws IOException {
		raf.seek(0);
		long pos = raf.readLong();
		return new BufferedInputStream(
			new BoundedInputStream(
				new CloseShieldInputStream(
					new FileInputStream(raf.getFD())
				),
				pos-Long.BYTES
			)
		);
	}
	
	public OutputStream writeContent() throws IOException {
		return new CloseShieldOutputStream(tmpOut);
	}
	
	public InputStream readContent() throws IOException {
		raf.seek(0);
		long pos = raf.readLong();
		raf.seek(pos);
		
		return new BufferedInputStream(
			new CloseShieldInputStream(
				new FileInputStream(raf.getFD())
			)
		);
	}
	
	@Override
	public void close() throws IOException {
		if(tmpOut!=null) {
			tmpOut.close();
			
			long pos = raf.getFilePointer();
			raf.seek(0);
			raf.writeLong(pos); //write the starting position of the content
			raf.seek(pos);
			try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmpFile));
					FileOutputStream out = new FileOutputStream(raf.getFD())) {
				IOUtils.copy(in, out);
			}
		}
		raf.close();
	}

	public long getHeaderSize() throws IOException {
		raf.seek(0);
		return raf.readLong();
	}

	public long getContentSize() throws IOException {
		return raf.length()-getHeaderSize();
	}
}
