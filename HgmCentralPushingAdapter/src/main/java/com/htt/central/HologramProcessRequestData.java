package com.htt.central;

import java.math.BigDecimal;

public class HologramProcessRequestData {

	private Long id;
	private String productionId;
	private Long productionUnitMasterId;
	private String productionUnitCode;
	private Long lineNameId;
	private String lineName;
	private Long companyMasterId;
	private String companyCode;
	private Long productMasterId;
	private String productCode;
	private String batchNumber;
	private String productCategory;
	private String approvedBy;
	private BigDecimal mrp;
	private BigDecimal msp;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProductionId() {
		return productionId;
	}

	public void setProductionId(String productionId) {
		this.productionId = productionId;
	}

	public Long getProductionUnitMasterId() {
		return productionUnitMasterId;
	}

	public void setProductionUnitMasterId(Long productionUnitMasterId) {
		this.productionUnitMasterId = productionUnitMasterId;
	}

	public String getProductionUnitCode() {
		return productionUnitCode;
	}

	public void setProductionUnitCode(String productionUnitCode) {
		this.productionUnitCode = productionUnitCode;
	}

	public Long getLineNameId() {
		return lineNameId;
	}

	public void setLineNameId(Long lineNameId) {
		this.lineNameId = lineNameId;
	}

	public String getLineName() {
		return lineName;
	}

	public void setLineName(String lineName) {
		this.lineName = lineName;
	}

	public Long getCompanyMasterId() {
		return companyMasterId;
	}

	public void setCompanyMasterId(Long companyMasterId) {
		this.companyMasterId = companyMasterId;
	}

	public String getCompanyCode() {
		return companyCode;
	}

	public void setCompanyCode(String companyCode) {
		this.companyCode = companyCode;
	}

	public Long getProductMasterId() {
		return productMasterId;
	}

	public void setProductMasterId(Long productMasterId) {
		this.productMasterId = productMasterId;
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	public String getBatchNumber() {
		return batchNumber;
	}

	public void setBatchNumber(String batchNumber) {
		this.batchNumber = batchNumber;
	}

	public String getProductCategory() {
		return productCategory;
	}

	public void setProductCategory(String productCategory) {
		this.productCategory = productCategory;
	}

	public String getApprovedBy() {
		return approvedBy;
	}

	public void setApprovedBy(String approvedBy) {
		this.approvedBy = approvedBy;
	}

	public BigDecimal getMrp() {
		return mrp;
	}

	public void setMrp(BigDecimal mrp) {
		this.mrp = mrp;
	}

	public BigDecimal getMsp() {
		return msp;
	}

	public void setMsp(BigDecimal msp) {
		this.msp = msp;
	}

}
