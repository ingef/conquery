package com.bakdata.conquery.util.io;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Joiner;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class FileTreeReduction {

	private final String name;
	private final File f;
	private final int depth;
	private final boolean file;
	private final String relativePath;
	private final List<FileTreeReduction> children = new ArrayList<>();

	public static List<FileTreeReduction> reduceByExtension(File root, String extension) {
		List<FileTreeReduction> children = new ArrayList<>();
		for (File file : root.listFiles()) {
			children.add(build(root.toPath(), file, 0));
		}

		List<FileTreeReduction> l = new ArrayList<>();
		Iterator<FileTreeReduction> it = children.iterator();
		while (it.hasNext()) {
			FileTreeReduction ftr = it.next();
			if (reduce(ftr, extension, l)) {
				it.remove();
			}
			else {
				l.add(ftr);
			}
		}
		Collections.reverse(l);

		return l;
	}

	private static boolean reduce(FileTreeReduction ftr, String extension, List<FileTreeReduction> l) {
		if (ftr.children.isEmpty()) {
			return !ftr.name.endsWith(extension);
		}
		else {
			boolean remove = true;
			Iterator<FileTreeReduction> it = ftr.children.iterator();
			while (it.hasNext()) {
				FileTreeReduction c = it.next();
				if (reduce(c, extension, l)) {
					it.remove();
				}
				else {
					remove = false;
					l.add(c);
				}
			}
			return remove;
		}
	}

	private static FileTreeReduction build(Path root, File file, int depth) {
		FileTreeReduction ftr = new FileTreeReduction(
			file.getName(),
			file,
			depth,
			file.isFile(),
			Joiner.on('/').join(root.relativize(file.toPath()).iterator()));
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				ftr.getChildren().add(build(root, f, depth + 1));
			}
		}
		return ftr;
	}
}
