package com.airqua.util;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;

public class AirquaUtils {

	public static ApplicationContext getApplicationContext(){
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
		return applicationContext;
	}

	public static boolean isNoDataMsgFound(JBrowserDriver driver, String xPathExp, String noDataMsg){

		if(isElementPresent(driver, xPathExp)){
			String cellData = driver.findElement(By.xpath(xPathExp)).getText();
			return cellData.equalsIgnoreCase(noDataMsg);
		}
		return false;
	}

	public static boolean isElementPresent(JBrowserDriver driver, String xPathExp){
		try{
			driver.findElement(By.xpath(xPathExp));
			return true;
		}
		catch(NoSuchElementException e){
			return false;
		}
	}

	public static boolean isElementDisabled(WebElement element){

		return !element.isEnabled() || (element.getAttribute("disabled")!=null && element.getAttribute("disabled").equals("disabled"));
	}
}
