/**
 * 
 */
package com.jeffshepherd.sortable;

import java.io.File;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 * Base class which uses Lucene for indexing and searching.
 * 
 * @author Jeff Shepherd
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
	
	/**
	 * @return Lucene Analyzer
	 */
	protected Analyzer getAnalyzer() {
		return analyzer;
	}
	
	/**
	 * @return the directory where the Lucene index is located
	 * @return
	 */
	protected File getIndexDirectory() {
		return indexDirectory;
	}
}
