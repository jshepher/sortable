/**
 * 
 */
package com.jeffshepherd.sortable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Test challenge for sortable.com
 * @author Jeff Shepherd
 *
 */
public class SortableTest extends JsonObjectManipulator {
	private final File products;
	private final File listings;
	private final File indexDirectory;
	
	public SortableTest(File productsFile, File listingsFile, File indexDirectory) {
		this.products = productsFile;
		this.listings = listingsFile;
		this.indexDirectory = indexDirectory;
	}
	/**
	 * @param args
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public static void main(String[] args) throws Exception {
		String products = args[0];
		String listings = args[1];
		
		new SortableTest(new File(products), new File(listings),
				new File(System.getProperty("user.home"))).run();
	}
	
	public void run() throws IOException {
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		ProductIndexer indexer = new ProductIndexer(indexDirectory);
		Reader productReader = new InputStreamReader(new FileInputStream(products), StandardCharsets.UTF_8);
		try {
			indexer.index(productReader);
		} finally {
			productReader.close();
		}
			
			
		Reader listingReader = new InputStreamReader(new FileInputStream(listings), StandardCharsets.UTF_8);
		try {
			ProductSearcher searcher = new ProductSearcher(indexDirectory);
			Collection<Result> results = searcher.searchForProductsGivenListings(listingReader);
			for (Result r : results) {
				System.out.println(getMapper().writeValueAsString(r));
			}
		} finally {
			listingReader.close();
		}
	}
}
