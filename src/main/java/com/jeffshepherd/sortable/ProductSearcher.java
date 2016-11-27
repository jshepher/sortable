package com.jeffshepherd.sortable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

/**
 * Class which searches for products given listings.
 * @author Jeff Shepherd
 *
 */
public class ProductSearcher extends LuceneIndexer implements IndexConstants {
	/**
	 * Boost the score if we get a match in the manufacturer 
	 * Manufacturer is the most important so it gets the biggest boost.
	 */
	private static final float MANUFACTURER_MATCH_BOOST = 2.8f;
	
	/**
	 * Boost the score if we get a match in the model
	 * Model is the second most important factor for a match
	 */
	private static final float MODEL_MATCH_BOOST = 1.5f;
	
	/**
	 * Boost the score if we get a match in the family
	 */
	private static final float FAMILY_MATCH_BOOST = 1.5f;
	
	/**
	 * Minimum score needed to be considered a match
	 */
	private static final float MATCHING_SCORE = 10.0f;
	
	/**
	 * If the 2 best matches differ by less than this score, 
	 * we don't consider it a match
	 */
	private static final float DIFFERING_SCORE = 2.0f;
	
	/**
	 * Maximum number of matches to return
	 */
	private static final int MAX_MATCHES = 5;
	
	/**
	 * Lucene index reader
	 */
	private final IndexReader indexReader;
	
	/**
	 * Lucene index searcher
	 */
	private final IndexSearcher searcher;
	
	/**
	 * Create a new ProductSearch object.
	 * @param indexDirectory where the document index is located
	 * @throws IOException
	 */
	public ProductSearcher(File indexDirectory) throws IOException {
		super(indexDirectory);
		indexReader = DirectoryReader.open(FSDirectory.open(getIndexDirectory().toPath()));
		searcher = new IndexSearcher(indexReader);
	}
	
	/**
	 * Match products with the given listings
	 * @param listings reader containing a stream of Listing JSON objects
	 * @return a list of Results containing the products that match the listing
	 * @throws IOException
	 */
	public Collection<Result> searchForProductsGivenListings(Reader listings) throws IOException {
		Map<String, Result> results = new HashMap<String, Result>();
		BufferedReader br = new BufferedReader(listings);
		Analyzer analyzer = getAnalyzer();
		String line;
		
		while ((line = br.readLine()) != null) {
			Listing listing = getMapper().readValue(line, Listing.class);
			TokenStream ts;
			BooleanQuery.Builder bqb = new BooleanQuery.Builder();

			// Generate lucene query given the manufacturer and title
			ts = analyzer.tokenStream(MANUFACTURER, listing.getManufacturer());
			CharTermAttribute cattr = ts.addAttribute(CharTermAttribute.class);
			
			ts.reset();
			BooleanQuery.Builder mbqb = new BooleanQuery.Builder();
			mbqb.setMinimumNumberShouldMatch(1);
			mbqb = new BooleanQuery.Builder();
			while (ts.incrementToken()) {
				mbqb.add(new BooleanClause(new TermQuery(new Term(MANUFACTURER, cattr.toString())), BooleanClause.Occur.SHOULD));
			}
			ts.close();
			bqb.add(new BoostQuery(mbqb.build(), MANUFACTURER_MATCH_BOOST), BooleanClause.Occur.MUST);
			

			ts = analyzer.tokenStream(PRODUCT_NAME, listing.getTitle());
			cattr = ts.addAttribute(CharTermAttribute.class);
			
			BooleanQuery.Builder bqbModel = new BooleanQuery.Builder();
			BooleanQuery.Builder bqbFamily = new BooleanQuery.Builder(); 
			bqbModel.setMinimumNumberShouldMatch(1);

			ts.reset();
			while (ts.incrementToken()) {
				// size == 1,2 letters, too small to search
				if (cattr.toString().length() >= 3) {					
					bqbFamily.add(new BooleanClause(new TermQuery(
							new Term(FAMILY, cattr.toString())), BooleanClause.Occur.SHOULD));
					bqbModel.add(new BooleanClause(new TermQuery(
							new Term(MODEL, cattr.toString())), BooleanClause.Occur.SHOULD));
				}
			}
			ts.close();
			
			bqb.add(new BoostQuery(bqbModel.build(), MODEL_MATCH_BOOST), BooleanClause.Occur.MUST);
			bqb.add(new BoostQuery(bqbFamily.build(), FAMILY_MATCH_BOOST), BooleanClause.Occur.SHOULD);			
			BooleanQuery bq = bqb.build();
			
			ScoreDoc[] hits = searcher.search(bq, MAX_MATCHES).scoreDocs;
			if (matches(hits)) {
			
				Document d = searcher.doc(hits[0].doc);
				IndexableField field = d.getField(PRODUCT_NAME);
				String productName = field.stringValue();
				Result hit = results.get(productName);
				if (hit == null) {
					hit = new Result();
					hit.setProductName(productName);
					results.put(productName, hit);
				}
				hit.addListing(listing);
			}
		}
		return results.values();
	}

	/**
	 * Figure out if the document matches the listing
	 * @param hits list of matching documents
	 * @return true if the document is a match, false otherwise
	 */
	private boolean matches(ScoreDoc[] hits) {
		if (hits.length == 0) {
			return false;
		} else {
			if (hits[0].score > MATCHING_SCORE) {
				if (hits.length > 1) {
					// If we have more than one doc and the scores are very similar
					// then be conservative and say it doesn't match
					if (hits[0].score - hits[1].score > DIFFERING_SCORE) {
						return true;
					} else {
						return false;
					}
				} else {
					return true;
				}
			} else {
				return false;
			}
		}
	}
}