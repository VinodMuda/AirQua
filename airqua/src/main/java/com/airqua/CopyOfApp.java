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

public class CopyOfApp {

	private static final Logger logger = LoggerFactory.getLogger(App.class);


	public static void main(String[] args) throws IOException, InterruptedException {

		long startTime = System.currentTimeMillis();

		logger.info("Airqua Started");

		JBrowserDriver driver = null;
		Long sleepTime = null;
		String noDataAvailableMsg = null;
		int stationsCompletedCount = 0;

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
						
						stationsCompletedCount++;

						Select paramDropdown = new Select(driver.findElement(By.id("lstBoxChannelLeft")));
						List<WebElement> paramOptionList = paramDropdown.getOptions();
						if(CollectionUtils.isNotEmpty(paramOptionList)){
							logger.info("List of available parameters:");
							for(WebElement paramOption : paramOptionList){
								logger.info(paramOption.getText());
							}
						}	

						String selectedParams = properties.getProperty(AirquaConstants.AIR_QUALITY_PARAMS);
						logger.info(String.format("Selected air quality parameters : %s", selectedParams));
						Thread.sleep(sleepTime);
						paramDropdown.selectByVisibleText(selectedParams);

						driver.findElement(By.id("btnAdd")).click();

						Thread.sleep(sleepTime);

						/*Select selectedParamDropdown = new Select(driver.findElement(By.id("lstBoxChannelRight")));
						List<WebElement> selectedParamOptions = selectedParamDropdown.getOptions();
						if(CollectionUtils.isNotEmpty(selectedParamOptions)){
							for(WebElement optionP : selectedParamOptions){
								logger.info(optionP.getText());
							}
						}*/

						driver.findElement(By.id("btnSubmit")).click();

						logger.info("Waiting for the response from the server...");

						//System.out.println(driver.getPageSource());

						if(selectedParams.equals(AirQualityParameters.ALL.getName())){
							/*
							String trXpath = "//*[@id='gvReportStation_ctl03_lblChannelData']/table/tbody/tr";
							List<WebElement> tableRows = driver.findElements(By.xpath(trXpath));
							logger.info(String.format("Number of rows found: %s", tableRows.size()));
							for(int i=1; i<tableRows.size()-1; i++)
							{
								WebElement rowElement = tableRows.get(i);
								List<WebElement> totalColumnCount=rowElement.findElements(By.xpath("td"));
								for(WebElement colElement : totalColumnCount)
								{
									logger.info(colElement.getText());
								}
							}
							 */

							boolean isFirstFetch = true;

							//There is an element on the page which represents the text "There is no data available!".
							String noDataXpath = "//*[@id='lblBlank']/table/tbody/tr[2]/td";

							//This will go into loop until the above the element is found. 
							while(!AirquaUtils.isNoDataMsgFound(driver, noDataXpath, noDataAvailableMsg)){

								//No need to click the next button for the first fetch.
								if(!isFirstFetch){
									String nextButtonXpath = "//*[@id='btnNext1']";
									if(AirquaUtils.isElementPresent(driver, nextButtonXpath)){
										WebElement nextButton = driver.findElement(By.xpath(nextButtonXpath));
										if(AirquaUtils.isElementDisabled(nextButton)){

											//This will break out of the loop, which means the anchor tag is disabled and there is no data available.
											break;
										}else{
											nextButton.click();
										}
									}
									else{
										break;
									}
								}
								isFirstFetch = false;

								List<WebElement> pageList = driver.findElements(By.xpath("//*[@id='gvReportStation']/tbody/tr[1]/td/table/tbody/tr/td"));
								int tabsPerPage = pageList.size();
								logger.info(String.format("Number of tabs per page: %s", tabsPerPage));

								//Sometimes, the tabs per page turns out to be 0, which generally means this the last page for the current station.
								boolean isLastPage = false;
								if(tabsPerPage<=0){
									tabsPerPage = 1;
									isLastPage = true;
								}

								for(int i=1; i<=tabsPerPage; i++){

									//This is to avoid clicking on span tag, as when the next button is clicked, then
									//by default the first tab is clicked. So, no need to click it again.
									/*if(i>1){
										String nextParamXpath = "//*[@id='gvReportStation']/tbody/tr[1]/td/table/tbody/tr/td["+i+"]/a";
										driver.findElement(By.xpath(nextParamXpath)).click();
									}*/

									String nextParamXpath = "//*[@id='gvReportStation']/tbody/tr[1]/td/table/tbody/tr/td["+i+"]/a";
									if(AirquaUtils.isElementPresent(driver, nextParamXpath)){
										driver.findElement(By.xpath(nextParamXpath)).click();
									}

									String trXpath = "//*[@id='gvReportStation_ctl03_lblChannelData']/table/tbody/tr";
									List<WebElement> tableRows = driver.findElements(By.xpath(trXpath));
									logger.info(String.format("%s Number of rows found on page %s:", tableRows.size(), i));
									for(int j=1; j<tableRows.size()-1; j++){

										WebElement rowElement = tableRows.get(j);
										List<WebElement> totalColumnCount=rowElement.findElements(By.xpath("td"));
										for(WebElement colElement : totalColumnCount){

											logger.info(colElement.getText());
										}
									}
								}
								//Or else it goes into infinite loop by trying to click on unanchored next button and landing no where.
								if(isLastPage){
									break;
								}
							}
						}

						//closing the page for the current station.
						String closeButtonXpath = "//*[@id='btnClose']";
						driver.findElementByXPath(closeButtonXpath).click();
						Thread.sleep(sleepTime);
						
						//Decreased the count by 1, because the list contains an invalid station, the "Select Station".
						if(CollectionUtils.isNotEmpty(stationList) && (stationList.size()-1)==stationsCompletedCount){
							break;
						}
						setupTargetForm(driver, sleepTime, properties);
					}
				}	
			}
		}
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
}
