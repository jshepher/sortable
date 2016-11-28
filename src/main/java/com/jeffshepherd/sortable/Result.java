package com.jeffshepherd.sortable;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 
 * Object holding the matched result
 * 
 * @author Jeff Shepherd
 */
public class Result {
	
	@JsonProperty("product_name")
	private String productName;
	
	@JsonProperty("listings")
	private List<Listing> listings = new ArrayList<Listing>();

	/**
	 * @return the productName
	 */
	public String getProductName() {
		return productName;
	}

	/**
	 * @param productName the productName to set
	 */
	public void setProductName(String productName) {
		this.productName = productName;
	}

	/**
	 * @return the listings
	 */
	public List<Listing> getListings() {
		return listings;
	}

	/**
	 * @param listings the listings to set
	 */
	public void addListing(Listing listing) {
		this.listings.add(listing);
	}
	
	@Override
	public int hashCode() {
		return productName.hashCode();
	}
	
	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object instanceof Result) {
			Result res = (Result)object;
			return res.productName.equals(productName);
		}
		return false;
	}
}
