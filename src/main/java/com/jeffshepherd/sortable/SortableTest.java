package com.jeffshepherd.sortable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * Test challenge for sortable.com
 * @author Jeff Shepherd
 *
 */
public class SortableTest extends JsonObjectManipulator {
	/**
	 * File containing a list of products in JSON format (Product object)
	 */
	private final File products;
	
	/**
	 * File containing a list of listings in JSON format (Listing object)
	 */
	private final File listings;
	
	/**
	 * Directory where to put the document index
	 */
	private final File indexDirectory;

	/**
	 * Construct a new SortableTest.
	 * @param productsFile file containing a list of Products
	 * @param listingsFile file containing a list of Listings
	 * @param indexDirectory directory where the document index will be stored
	 */
	public SortableTest(File productsFile, File listingsFile, File indexDirectory) {
		this.products = productsFile;
		this.listings = listingsFile;
		this.indexDirectory = indexDirectory;
	}

	/**
	 * Main method
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: com.jeffshepherd.sortable.SortableTest <products file> <listings file>");
			System.exit(1);
		}
		String products = args[0];
		String listings = args[1];
		
		new SortableTest(new File(products), new File(listings),
				new File(System.getProperty("user.home"))).run();
	}
	

	/**
	 * Run the sortable.com test and print out the results
	 * @throws IOException
	 */
	public void run() throws IOException {
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
				System.out.println();
			}
		} finally {
			listingReader.close();
		}
	}
}
