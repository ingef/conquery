package com.bakdata.conquery.test.page;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

/**
 *
 * @author Marcus Baitz
 */
public class PreviousQueries {

	private final WebDriver driver;

	public PreviousQueries(WebDriver driver) {
		this.driver = driver;
		PageFactory.initElements(driver, this);
	}

	public WebElement getPreviousQuery(int idx) {
		return getPreviousQueries().get(idx);
	}

	public List<WebElement> getPreviousQueries() {
		return driver.findElements(By.xpath("//div[@class='previous-queries']//div[@class='previous-query-container']"));
	}
}
