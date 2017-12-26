package com.jsontordbms.ija;

public class JsonRecord {
	
	String name;
	String description;
	String category;
	int offersTotal;
	
	public JsonRecord() {}
	
	public JsonRecord(String name, String description, String category, int offersTotal) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.offersTotal = offersTotal;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}
	
	public int getOffersTotal() {
		return offersTotal;
	}
	
	public void setOffersTotal(int offersTotal) {
		this.offersTotal = offersTotal;
	}

}
