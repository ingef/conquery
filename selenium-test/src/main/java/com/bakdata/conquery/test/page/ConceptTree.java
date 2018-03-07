package com.bakdata.conquery.test.page;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author Marcus Baitz
 */
public class ConceptTree {

    public static final String XPATH_TREE = "//div[@class='category-tree-list']";

    @FindBy(xpath = "//input[@type='hidden'][@name='dataset-selector']")
    private WebElement datasetSelect;

    @FindBy(xpath = XPATH_TREE)
    private WebElement treeRoot;
    
    private final WebDriver driver;

    public ConceptTree(WebDriver driver) {
        this.driver = driver;
        
        PageFactory.initElements(driver, this);
    }

    public ConceptTree datasetSelectByValue(String value) {
        new Select(datasetSelect).selectByValue(value);

        return this;
    }

    public WebElement getDatasetSelect() {
        return datasetSelect;
    }

    public WebElement getTreeRoot() {
        return treeRoot;
    }
    
    public WebElement getTreeNode(String name) {
        return getTreeNode(name, false);
   }
    
    public WebElement getTreeNode(String name, boolean open) {
        String xpathNode = open 
                ? XPATH_TREE + "//p[contains(.,'%s')]/i" 
                : XPATH_TREE + "//p[contains(.,'%s')]";
        WebElement node = driver.findElement(By.xpath(String.format(xpathNode, name)));
        new WebDriverWait(driver, 10).until(ExpectedConditions.elementToBeClickable(node));
        
        return node;
    }
    
}
