package com.smartfarm.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "houses")
public class House {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 8)
	private String code;

	@Column(nullable = false, length = 40)
	private String name;

	@Column(name = "crop_code", nullable = false, unique = true, length = 32)
	private String cropCode;

	@Column(name = "crop_name", nullable = false, length = 40)
	private String cropName;

	@Column(name = "day_temp", nullable = false, length = 40)
	private String dayTemp;

	@Column(name = "night_temp", nullable = false, length = 40)
	private String nightTemp;

	@Column(name = "light_note", nullable = false, length = 80)
	private String lightNote;

	protected House() {
	}

	public House(String code, String name, String cropCode, String cropName, String dayTemp, String nightTemp, String lightNote) {
		this.code = code;
		this.name = name;
		this.cropCode = cropCode;
		this.cropName = cropName;
		this.dayTemp = dayTemp;
		this.nightTemp = nightTemp;
		this.lightNote = lightNote;
	}

	public void rename(String name, String cropName, String dayTemp, String nightTemp, String lightNote) {
		this.name = name;
		this.cropName = cropName;
		this.dayTemp = dayTemp;
		this.nightTemp = nightTemp;
		this.lightNote = lightNote;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getCropCode() {
		return cropCode;
	}

	public String getCropName() {
		return cropName;
	}

	public String getDayTemp() {
		return dayTemp;
	}

	public String getNightTemp() {
		return nightTemp;
	}

	public String getLightNote() {
		return lightNote;
	}
}
