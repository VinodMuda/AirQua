package com.airqua;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airqua.util.AirquaUtils;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;


/**
 * This application fetches air quality data from Central Pollution Control Board(http://www.cpcb.gov.in),
 * Ministry of Environment & Forests, Govt of India
 * and generates the result in the CSV format, so that it can be further used for research purpose.
 * 
 * @author Vinod Muda
 * @since 1.0
 */

public class App {

	private static final Logger logger = LoggerFactory.getLogger(App.class);


	public static void main(String[] args) throws IOException, InterruptedException {

		long startTime = System.currentTimeMillis();

		logger.info("Airqua Started");

		JBrowserDriver driver = null;
		Long sleepTime = null;
		String noDataAvailableMsg = null;
		
		try{

			Properties properties = AirquaUtils.getApplicationContext().getBean("airquaProperties", Properties.class);

			sleepTime = Long.valueOf(properties.getProperty(AirquaConstants.SLEEP_TIME));
			logger.info(String.format("Configured sleep time: %s", sleepTime));
			noDataAvailableMsg = properties.getProperty(AirquaConstants.NO_DATA_AVAILABLE_MSG);
			logger.info("Initializing browser driver...");
			driver = new JBrowserDriver(Settings.builder().timezone(Timezone.ASIA_CALCUTTA).build());
			logger.info("Browser driver initialization completed");

			setupTargetForm(driver, sleepTime, properties);

			Select stationDropdown = new Select(driver.findElement(By.id("ddlStation")));
			List<WebElement> stationOptions = stationDropdown.getOptions();
			String selectedStation = properties.getProperty(AirquaConstants.STATION_VALUE);

			if(selectedStation.equals(AirquaConstants.ALL_AVAILABLE_STATIONS)){

				List<String> stationList = new ArrayList<String>();

				logger.info("List of stations available for the selected city:");

				if(CollectionUtils.isNotEmpty(stationOptions)){
					for(WebElement option: stationOptions){
						String stationName = option.getText();
						stationList.add(stationName);
						logger.info(stationName);
					}
				}

				if(CollectionUtils.isNotEmpty(stationList)){
					for(String stationName : stationList){

						String selectedCity = properties.getProperty(AirquaConstants.CITY_VALUE);

						if(StringUtils.isBlank(stationName) || stationName.equals(AirquaConstants.SELECT_STATION)){
							continue;
						}
						logger.info(String.format("Fetching data for station: %s, %s", stationName, selectedCity));
						stationDropdown = new Select(driver.findElement(By.id("ddlStation")));
						stationDropdown.selectByVisibleText(stationName);
						
						Select paramDropdown = new Select(driver.findElement(By.id("lstBoxChannelLeft")));
						List<WebElement> paramOptionList = paramDropdown.getOptions();
						List<String> paramList = new ArrayList<String>();
						if(CollectionUtils.isNotEmpty(paramOptionList)){
							logger.info("List of available parameters:");
							for(WebElement paramOption : paramOptionList){
								logger.info(paramOption.getText());
								paramList.add(paramOption.getText());
							}
						}	
						
						for(String param : paramList){
							
							setupTargetForm(driver, sleepTime, properties);
						
							stationDropdown = new Select(driver.findElement(By.id("ddlStation")));
							stationDropdown.selectByVisibleText(stationName);


							if(CollectionUtils.isNotEmpty(stationOptions)){
								for(WebElement option: stationOptions){
								}
							}
							
							logger.info(String.format("Fetching data of air quality parameter : %s", param));
							
							paramDropdown = new Select(driver.findElement(By.id("lstBoxChannelLeft")));
							paramDropdown.selectByVisibleText(param);

							Thread.sleep(sleepTime);

							driver.findElement(By.id("btnAdd")).click();

							Thread.sleep(sleepTime);
							driver.findElement(By.id("btnSubmit")).click();

							logger.info("Waiting for the response from the server...");
							
							String noDataXpath = "//*[@id='lblBlank']/table/tbody/tr[2]/td";
							if(AirquaUtils.isNoDataMsgFound(driver, noDataXpath, noDataAvailableMsg)){
								
								clickCloseButton(driver);
								Thread.sleep(sleepTime);
								continue;
							}
							
							
							String trXpath = "//*[@id='gvReportStation_ctl03_lblChannelData']/table/tbody/tr";
							List<WebElement> tableRows = driver.findElements(By.xpath(trXpath));
							logger.info(String.format("%s Number of rows found for parameter %s:", tableRows.size(), param));
							for(int j=1; j<tableRows.size()-1; j++){

								WebElement rowElement = tableRows.get(j);
								List<WebElement> totalColumnCount=rowElement.findElements(By.xpath("td"));
								for(WebElement colElement : totalColumnCount){

									logger.info(colElement.getText());
								}
							}
							
							clickCloseButton(driver);
							Thread.sleep(sleepTime);
							
							
						}
					}}}}
		finally {
			driver.quit();
			long endTime = System.currentTimeMillis();
			logger.info(String.format("Total time take to execute: %s seconds",  (endTime - startTime)/1000));
		}
	}


	private static void setupTargetForm(JBrowserDriver driver,
			Long sleepTime, Properties properties) throws InterruptedException {

		String targetURL = properties.getProperty(AirquaConstants.TARTGET_URL); 
		logger.info(String.format("Airqua hitting the target URL : %s", targetURL));
		driver.get(targetURL);

		String startRangeXpath = "//*[@id='txtDateFrom']";
		WebElement startRangeField = driver.findElement(By.xpath(startRangeXpath));

		String startRangeValue = properties.getProperty(AirquaConstants.START_DATE);
		logger.info(String.format("Start range value : %s", startRangeValue));
		startRangeField.sendKeys(startRangeValue);

		String endRangeXpath = "//*[@id='txtDateTo']";
		WebElement endRangeField = driver.findElement(By.xpath(endRangeXpath));

		String endRangeValue = properties.getProperty(AirquaConstants.END_DATE);
		logger.info(String.format("End range value : %s", endRangeValue));
		endRangeField.sendKeys(endRangeValue);

		Select stateDropdown = new Select(driver.findElement(By.id("ddlState")));
		String selectedState = properties.getProperty(AirquaConstants.STATE_VALUE);
		logger.info(String.format("Selected State : %s", selectedState));
		stateDropdown.selectByVisibleText(selectedState);

		//Temporary sleep is required, because the page gets refreshed at this point after selecting the drop down.
		Thread.sleep(sleepTime);

		Select cityDropdown = new Select(driver.findElement(By.id("ddlCity")));

		logger.info("List of cities available for the selected state:");
		List<WebElement> cityOptions = cityDropdown.getOptions();
		if(CollectionUtils.isNotEmpty(cityOptions)){
			for(WebElement option : cityOptions){
				logger.info(option.getText());
			}
		}		

		String selectedCity = properties.getProperty(AirquaConstants.CITY_VALUE); 
		logger.info(String.format("Selected City : %s", selectedCity));
		cityDropdown.selectByVisibleText(selectedCity);

		Thread.sleep(sleepTime);
	}
	
	public static void clickCloseButton(JBrowserDriver driver){
		String closeButtonXpath = "//*[@id='btnClose']";
		driver.findElementByXPath(closeButtonXpath).click();
	}
}
