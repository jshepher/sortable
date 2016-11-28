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
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
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
	 * Boost given to phrase matching -- usually multiplied
	 * by the number of tokens matched
	 */
	private static final float PHRASE_MATCH_BOOST = /*1.2f*/1f;

	
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
	 * Create a query that tries to match phrases for a certain string of text.
	 * @param fieldName lucence search field name
	 * @param fieldValue value to try to match in searches
	 * @return Query representing the search
	 * @throws IOException
	 */
	private Query createPhraseQuery(String fieldName, String fieldValue) throws IOException {
		BooleanQuery.Builder bqb = new BooleanQuery.Builder(); 
		bqb.setMinimumNumberShouldMatch(1);
		int numSearchTokens = 0;
		TokenStream ts = getAnalyzer().tokenStream(fieldName, fieldValue);
		try {
			ts.reset();
			CharTermAttribute cattr = ts.addAttribute(CharTermAttribute.class);
			StringBuilder sb = new StringBuilder();
			while (ts.incrementToken()) {
				String token = cattr.toString();
				// Anything less than 2 characters isn't worth searching for.
				if (token.length() >= 2) {	
					sb.append(cattr.toString());
					// Don't need a 1-token search
					if (numSearchTokens > 0) {
						PhraseQuery.Builder pqb = new PhraseQuery.Builder().setSlop(numSearchTokens/2);
						pqb.add(new Term(fieldName, sb.toString()));
						// For the longer phrase queries if it matches it indicates a higher score
						bqb.add(new BooleanClause(new BoostQuery(pqb.build(), PHRASE_MATCH_BOOST * numSearchTokens), 
								BooleanClause.Occur.SHOULD));
					}
					sb.append(' ');
					numSearchTokens++;
				}
			}
			return bqb.build();
		} finally {			
			ts.close();
		}
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
			BooleanQuery.Builder mbqb = new BooleanQuery.Builder();
			
			try {
				ts.reset();
				mbqb.setMinimumNumberShouldMatch(1);
				mbqb = new BooleanQuery.Builder();
				while (ts.incrementToken()) {
					mbqb.add(new BooleanClause(new TermQuery(new Term(MANUFACTURER, cattr.toString())), BooleanClause.Occur.SHOULD));
				}
			} finally {
				ts.close();
			}
			bqb.add(mbqb.build(), BooleanClause.Occur.MUST);
			

			ts = analyzer.tokenStream(PRODUCT_NAME, listing.getTitle());
			cattr = ts.addAttribute(CharTermAttribute.class);
			
			BooleanQuery.Builder bqbModel = new BooleanQuery.Builder();
			BooleanQuery.Builder bqbFamily = new BooleanQuery.Builder();

			int numSearchTokens = 0;
			try {
				ts.reset();
				while (ts.incrementToken()) {
					String token = cattr.toString();
					// size == 1,2 letters, too small to search
					if (token.length() >= 2) {					
						bqbFamily.add(new BooleanClause(new TermQuery(
								new Term(FAMILY, token)), BooleanClause.Occur.SHOULD));
						bqbModel.add(new BooleanClause(new TermQuery(
								new Term(MODEL, token)), BooleanClause.Occur.SHOULD));
						numSearchTokens++;
					}				
				}
			} finally {
				ts.close();
			}
			
			Query productbq = createPhraseQuery(PRODUCT_NAME, listing.getTitle());
			// TODO: If family and model tend to be phrases we could use createPhraseQuery there too
			bqb.add(bqbModel.build(), BooleanClause.Occur.MUST);
			bqb.add(bqbFamily.build(), BooleanClause.Occur.SHOULD);
			bqb.add(productbq, BooleanClause.Occur.SHOULD);
			BooleanQuery bq = bqb.build();

			ScoreDoc[] hits = searcher.search(bq, MAX_MATCHES).scoreDocs;
			if (matches(numSearchTokens, hits)) {
			
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
	 * Figure out the minimum score the hit should have
	 * @param numSearchTokens number of tokens that was searched for
	 * @return score
	 */
	protected float computeScore(int numSearchTokens) {
		return numSearchTokens / 2.0f;
	}
	
	/**
	 * Figure out if the document matches the listing
	 * @param hits list of matching documents
	 * @return true if the document is a match, false otherwise
	 */
	protected boolean matches(int numSearchTokens, ScoreDoc[] hits) {
		if (hits.length == 0) {
			return false;
		} else {
			if (hits[0].score > computeScore(numSearchTokens)) {
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