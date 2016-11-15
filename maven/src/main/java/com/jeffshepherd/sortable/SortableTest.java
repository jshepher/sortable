/**
 * 
 */
package com.jeffshepherd.sortable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

/**
 * @author Jeff Shepherd
 *
 */
public class SortableTest {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public static void main(String[] args) throws Exception {
		String products = args[0];
		String listings = args[1];
		
		BufferedReader br = null;

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JsonOrgModule());

		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		Path indexRoot = Paths.get(System.getProperty("user.home"));
		IndexWriter writer = new IndexWriter(FSDirectory.open(indexRoot), iwc);
		
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(products)), StandardCharsets.UTF_8));
			String line;
			while ((line = br.readLine()) != null) {
				Product p = mapper.readValue(line, Product.class);
				//System.out.println(p);
				Document d = new Document();
				d.add(new TextField("productName", p.getProductName(), Store.YES));
				d.add(new TextField("manufacturer", p.getManufacturer(), Store.YES));
				d.add(new TextField("model", p.getModel(), Store.YES));
				if (p.getFamily() != null)
					d.add(new TextField("family", p.getFamily(), Store.YES));
				writer.addDocument(d);
			}
			writer.close();
			
			br.close();
			br = null;
			
			br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(listings)), StandardCharsets.UTF_8));
			
			IndexReader reader = DirectoryReader.open(FSDirectory.open(indexRoot));
			IndexSearcher searcher = new IndexSearcher(reader);
			Map<String, Result> results = new HashMap<String, Result>();
			while ((line = br.readLine()) != null) {
				Listing listing = mapper.readValue(line, Listing.class);
				TokenStream ts = analyzer.tokenStream("manufacturer", listing.getManufacturer());
				CharTermAttribute cattr = ts.addAttribute(CharTermAttribute.class);
				BooleanQuery.Builder bqb = new BooleanQuery.Builder();
				ts.reset();
				while (ts.incrementToken()) {
					cattr.toString();
					bqb.add(new BooleanClause(new BoostQuery(new TermQuery(new Term("manufacturer", cattr.toString())), 0.1f), BooleanClause.Occur.SHOULD)); 
				}
				ts.close();
				
				ts = analyzer.tokenStream("productName", listing.getTitle());
				cattr = ts.addAttribute(CharTermAttribute.class);
				ts.reset();
				while (ts.incrementToken()) {
					// size == 1 means 1 letter, meaningless to search
					if (cattr.toString().length() >= 2) {
						bqb.add(new BooleanClause(new TermQuery(new Term("product_name", cattr.toString())), BooleanClause.Occur.SHOULD));
						bqb.add(new BooleanClause(new BoostQuery(new TermQuery(new Term("model", cattr.toString())), 1.1f), BooleanClause.Occur.SHOULD)); 
						bqb.add(new BooleanClause(new BoostQuery(new TermQuery(new Term("family", cattr.toString())), 1.5f), BooleanClause.Occur.SHOULD));
					}
				}
				ts.close();
				BooleanQuery bq = bqb.build();
				ScoreDoc[] hits = searcher.search(bq, 10).scoreDocs;
				if (hits.length > 0 && hits[0].score > 10.0f) {
					Document d = searcher.doc(hits[0].doc);
					IndexableField field = d.getField("productName");
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
			
			//System.out.println("{");
			for (Result r : results.values()) {
				System.out.println(mapper.writeValueAsString(r));
			}
			//System.out.println("}");
		} finally {
			if (br != null)
				br.close();
		}
	}

}
