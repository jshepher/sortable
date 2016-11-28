/**
 * 
 */
package com.jeffshepherd.sortable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;

/**
 * Class which takes as input a list of Products and indexes them.
 * @author Jeff Shepherd
 */
public class ProductIndexer extends LuceneIndexer implements IndexConstants {
	/**
	 * Standard index configuration for Lucene
	 */
	private final IndexWriterConfig iwc;

	public ProductIndexer(File indexDirectory) {
		super(indexDirectory);
		iwc = new IndexWriterConfig(getAnalyzer());
		iwc.setOpenMode(OpenMode.CREATE);
	}
	
	private String fixModel(String model) {
		StringTokenizer st = new StringTokenizer(model, " ");
		StringBuilder sb = new StringBuilder();
		if (st.countTokens() > 1) {
			while(st.hasMoreTokens()) {
				sb.append(st.nextToken());
			}
			return sb.toString();
		}
		return null;
	}
	
	private void parseField(Document document, String fieldName, String fieldValue) throws IOException {
		parseField(document, fieldName, fieldValue, false);
	}
	
	private void parseField(Document document, String fieldName, String fieldValue, boolean extraTokenize) throws IOException {
		if (fieldValue != null) {
			document.add(new TextField(fieldName, fieldValue, Store.YES));
			if (extraTokenize) {
				StringTokenizer st = new StringTokenizer(fieldValue," \t\n\r\f_-");
				StringBuilder sb = new StringBuilder();
				while (st.hasMoreTokens()) {
					//document.add(new TextField(fieldName, st.nextToken(), Store.YES));
					sb.append(st.nextToken());
					if (st.hasMoreTokens()) {
						sb.append(' ');
					}
				}
				document.add(new TextField(fieldName, sb.toString(), Store.YES));
			}
		}
	}
	
	/**
	 * Create an index for a list of Products
	 * @param products stream containing Products as JSON
	 * @throws IOException
	 */
	public void index(Reader products) throws IOException {
		BufferedReader br = null;
		IndexWriter writer = new IndexWriter(FSDirectory.open(getIndexDirectory().toPath()), iwc);
		try {
			br = new BufferedReader(products);
			String line;
			while ((line = br.readLine()) != null) {
				Product p = getMapper().readValue(line, Product.class);
				Document d = new Document();
				parseField(d, PRODUCT_NAME, p.getProductName(), true);
				parseField(d, MANUFACTURER, p.getManufacturer());
				parseField(d, FAMILY, p.getFamily());
				parseField(d, MODEL, p.getModel());
				// Sometimes the model is 1 or more words. Combine them into 1 word for indexing
				parseField(d, MODEL,fixModel(p.getModel()));
				writer.addDocument(d);
			}
		} finally {
			try {		
				writer.close();
			} catch (IOException ie) {
				// ignore
			}		
		}
	}
}
