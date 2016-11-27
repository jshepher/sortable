/**
 * 
 */
package com.jeffshepherd.sortable;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Product
 * @author Jeff Shepherd
 *
 */
public class Product {
	
	@JsonProperty("product_name")
	private String productName;
	
	@JsonProperty("manufacturer")
	private String manufacturer;
	
	@JsonProperty("model")
	private String model;
	
	@JsonProperty("announced-date")
	private Date announceDate;
	
	@JsonProperty("family")
	private String family;

	public String getProductName() {
		return productName;
	}

	public void setProductName(String product_Name) {
		this.productName = product_Name;
	}

	/**
	 * @return the manufacturer
	 */
	public String getManufacturer() {
		return manufacturer;
	}

	/**
	 * @param manufacturer the manufacturer to set
	 */
	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	/**
	 * @return the model
	 */
	public String getModel() {
		return model;
	}

	/**
	 * @param model the model to set
	 */
	public void setModel(String model) {
		this.model = model;
	}

	/**
	 * @return the announceDate
	 */
	public Date getAnnounceDate() {
		return announceDate;
	}

	/**
	 * @param announceDate the announceDate to set
	 */
	public void setAnnounceDate(Date announceDate) {
		this.announceDate = announceDate;
	}

	/**
	 * @return the family
	 */
	public String getFamily() {
		return family;
	}

	/**
	 * @param family the family to set
	 */
	public void setFamily(String family) {
		this.family = family;
	}	
}
