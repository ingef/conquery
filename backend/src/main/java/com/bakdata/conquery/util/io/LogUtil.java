package com.bakdata.conquery.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import com.github.powerlibraries.io.Out;
import com.github.powerlibraries.io.helper.stringout.SBPrintWriter;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LogUtil {

	public String printPath(File f) {
		return printPath(f.toPath());
	}
	
	public String printPath(Path p) {
		try {
			return p.toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
		} catch (IOException e) {
			return p.toAbsolutePath().toString();
		}
	}
	
	public String printException(Exception e) {
		try {
			SBPrintWriter errorPrinter = Out.string().asPrint();
			e.printStackTrace(errorPrinter);
			errorPrinter.close();
			return errorPrinter.getResult();
		} catch (IOException e1) {
			throw new IllegalStateException(e1);
		}
		
	}
}
