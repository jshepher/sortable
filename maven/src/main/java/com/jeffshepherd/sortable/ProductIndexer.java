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
				d.add(new TextField(PRODUCT_NAME, p.getProductName(), Store.YES));
				d.add(new TextField(MANUFACTURER, p.getManufacturer(), Store.YES));
				d.add(new TextField(MODEL, p.getModel(), Store.YES));
				// Sometimes the model is 1 or more words. Combine them into 1 word for indexing
				String fixedModel = fixModel(p.getModel());
				if (fixedModel != null)
					d.add(new TextField(MODEL, fixedModel, Store.YES));
				if (p.getFamily() != null)
					d.add(new TextField(FAMILY, p.getFamily(), Store.YES));
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
