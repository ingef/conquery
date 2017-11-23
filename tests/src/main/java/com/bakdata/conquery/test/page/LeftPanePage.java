package com.bakdata.conquery.test.page;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author Marcus Baitz
 */
public class LeftPanePage {
    
    private final WebDriver driver;
    private final WebDriverWait wait;

    public LeftPanePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
        this.wait = new WebDriverWait(driver, 30);
    }
    
    @FindBy(xpath = "//h2[contains(.,'Anfragen')]")
    private WebElement tabRequests;
    
    @FindBy(xpath = "//div[class='root']")
    private WebElement rootDiv;
    
    
    public void toTabRequests() {
        wait.until(ExpectedConditions.elementToBeClickable(this.tabRequests));
        this.tabRequests.click();
    }
    
}
