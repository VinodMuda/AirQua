package com.airqua;

public enum AirQualityParameters {

	ALL("All"),
	PM10("PM10"),
	SO2("SO2"),
	NOX("NOx"),
	CO("CO"),
	OZONE("Ozone"),
	WS("WS"),
	RH("RH"),
	BP("BP"),
	VWS("VWS"),
	SR("SR"),
	RF("RF"),
	BENZENE("Benzene"),
	TOLUENE("Toluene"),
	ETHBENZENE("EthBenzene"),
	MPXYLENE("MPXylene"),
	XYLENE("Xylene"),
	CH4("CH4"),
	WD("WD"),
	TEMP("Temp"),
	NO("NO"),
	NO2("NO2"),
	AT("AT"),
	NH3("NH3"),
	PM25("PM2.5");

	private String name;
	
	private AirQualityParameters(String name){
		this.setName(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
