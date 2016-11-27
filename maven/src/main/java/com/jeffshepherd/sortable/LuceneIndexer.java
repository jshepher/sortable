/**
 * 
 */
package com.jeffshepherd.sortable;

import java.io.File;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 * @author jshepher
 *
 */
public abstract class LuceneIndexer extends JsonObjectManipulator {
	/**
	 * Directory where the document/product index will be created
	 */
	private final File indexDirectory;
	
	/**
	 * Standard document analyzer
	 */
	private final Analyzer analyzer = new StandardAnalyzer();
	
	protected LuceneIndexer(File indexDirectory) {
		this.indexDirectory = indexDirectory;
	}
	
	protected Analyzer getAnalyzer() {
		return analyzer;
	}
	
	protected File getIndexDirectory() {
		return indexDirectory;
	}
}
