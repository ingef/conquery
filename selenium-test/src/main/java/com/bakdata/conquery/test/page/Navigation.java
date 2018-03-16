package com.bakdata.conquery.test.page;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

/**
 *
 * @author Marcus Baitz
 */
public class Navigation {
    
    public enum Pane {
        LEFT,RIGHT;
    }
    
    
    private final WebDriver driver;

    public Navigation(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }
    
    public WebElement getNavigateTo(String name) {
        return driver.findElement(By.xpath(String.format("//div[@class='pane-tab-navigation']//h2[text()='%s']", name)));
    }
    
}
