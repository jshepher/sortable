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

public class ProductSearcher extends LuceneIndexer implements IndexConstants {
	private static final float MANUFACTURER_MATCH_BOOST = 2.8f;
	private static final float MODEL_MATCH_BOOST = 1.5f;
	private static final float FAMILY_MATCH_BOOST = 1.5f;
	private static final float MATCHING_SCORE = 10f;
	private static final int MAX_MATCHES = 100;
	
	private final IndexReader indexReader;
	private final IndexSearcher searcher;
	
	public ProductSearcher(File indexDirectory) throws IOException {
		super(indexDirectory);
		indexReader = DirectoryReader.open(FSDirectory.open(getIndexDirectory().toPath()));
		searcher = new IndexSearcher(indexReader);
	}
	
	public Collection<Result> searchForProductsGivenListings(Reader listings) throws IOException {
		Map<String, Result> results = new HashMap<String, Result>();
		BufferedReader br = new BufferedReader(listings);
		Analyzer analyzer = getAnalyzer();
		String line;
		
		while ((line = br.readLine()) != null) {
			Listing listing = getMapper().readValue(line, Listing.class);
			TokenStream ts;
			BooleanQuery.Builder bqb = new BooleanQuery.Builder();

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
			BooleanQuery.Builder bqbProd = new BooleanQuery.Builder();
			BooleanQuery.Builder bqbModel = new BooleanQuery.Builder();
			BooleanQuery.Builder bqbFamily = new BooleanQuery.Builder(); 
			bqbModel.setMinimumNumberShouldMatch(1);
			bqbProd.setMinimumNumberShouldMatch(1);
			// Family is optional

			ts.reset();
			while (ts.incrementToken()) {
				// size == 1 means 1 letter, meaningless to search
				if (cattr.toString().length() >= 2) {					
					bqbProd.add(new BooleanClause(new TermQuery(
							new Term(PRODUCT_NAME, cattr.toString())), BooleanClause.Occur.SHOULD));
					bqbFamily.add(new BooleanClause(new TermQuery(
							new Term(FAMILY, cattr.toString())), BooleanClause.Occur.SHOULD));
					bqbModel.add(new BooleanClause(new TermQuery(
							new Term(MODEL, cattr.toString())), BooleanClause.Occur.SHOULD));
							
				}
			}
			ts.close();
			
			//bqb.add(bqbProd.build(), BooleanClause.Occur.SHOULD);			
			bqb.add(new BoostQuery(bqbModel.build(), MODEL_MATCH_BOOST), BooleanClause.Occur.MUST);
			bqb.add(new BoostQuery(bqbFamily.build(), FAMILY_MATCH_BOOST), BooleanClause.Occur.SHOULD);			
			BooleanQuery bq = bqb.build();
			
			ScoreDoc[] hits = searcher.search(bq, MAX_MATCHES).scoreDocs;
			if (hits.length > 0 && hits[0].score > MATCHING_SCORE) {
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
}