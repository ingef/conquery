package com.bakdata.conquery.test.page;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 *
 * @author Marcus Baitz
 */
public class GroupEditor {

	public static final String XPATH_AND_DROPZONE = "(%s//*[@class='query-group-connector'])[%d]/following-sibling::div//div[@class='dropzone']";
	public static final String XPATH_OR_DROPZONE = "(%s//*[@class='query-or-connector'])[%d]/preceding-sibling::div//div[@class='dropzone']";
	public static final String XPATH_FOLLOWING_SIBLING_DROPZONE = "/following-sibling::div//div[@class='dropzone']";
	public static final String XPATH_PARENT_TEXT = "/../..//span[contains(.,'%s')]";
	public static final String XPATH_GROUP_EDITOR = "//div[@class='query-editor']";
	public static final String XPATH_GROUP_EDITOR_INITIAL_DROPZONE = "//div[contains(@class,'query-editor-dropzone--initial')]";

	@FindBy(xpath = XPATH_GROUP_EDITOR_INITIAL_DROPZONE)
	private WebElement groupEditorInitialDropzone;

	@FindBy(xpath = XPATH_GROUP_EDITOR)
	private WebElement rootContext;

	@FindBy(xpath = "//div[@class='query-runner']//button[@class='query-runner-button']")
	private WebElement button;

	private final WebDriver driver;

	public GroupEditor(WebDriver driver) {
		this.driver = driver;
		PageFactory.initElements(driver, this);
	}

	public WebElement getRootContext() {
		return rootContext;
	}

	public WebElement getButton() {
		return button;
	}

	public WebElement getGroupEditorInitialDropzone() {
		return groupEditorInitialDropzone;
	}

	public WebElement getAndDropZone(int andIdx) {
		return driver.findElement(By.xpath(String.format(XPATH_AND_DROPZONE, XPATH_GROUP_EDITOR, andIdx)));
	}

	public WebElement getOrDropZone(int orIdx) {
		return driver.findElement(By.xpath(String.format(XPATH_OR_DROPZONE, XPATH_GROUP_EDITOR, orIdx)));
	}

	public WebElement getOrElement(String text, int orIdx) {
		return getOrElements(text, orIdx).get(0);
	}

	public List<WebElement> getOrElements(String text, int orIdx) {
		return driver.findElements(By.xpath(String.format(XPATH_OR_DROPZONE + XPATH_PARENT_TEXT, XPATH_GROUP_EDITOR, orIdx, text)));
	}

	public WebElement getAndElement(String text, int andIdx) {
		return getAndElements(text, andIdx).get(0);
	}

	public List<WebElement> getAndElements(String text, int andIdx) {
		return driver.findElements(By.xpath(String.format(XPATH_AND_DROPZONE + XPATH_PARENT_TEXT, XPATH_GROUP_EDITOR, andIdx, text)));
	}
}
