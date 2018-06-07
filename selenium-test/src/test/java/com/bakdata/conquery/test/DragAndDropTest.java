package com.bakdata.conquery.test;

import com.bakdata.conquery.test.page.ConceptTree;
import com.bakdata.conquery.test.page.GroupEditor;
import com.bakdata.conquery.test.util.DragDropHelper;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author Marcus Baitz
 */
public class DragAndDropTest {

    private WebDriver driver;
    private GroupEditor groupEditor;
    private ConceptTree conceptTree;

    @Before
    public void setUp() throws MalformedURLException {

        String testLocal = System.getProperty("testLocal", "false");
        if (Boolean.parseBoolean(testLocal)) {
            System.setProperty("webdriver.chrome.driver", Paths.get("src/test/resources/driver/chromedriver").toString());
            driver = new ChromeDriver();
            driver.get("http://localhost:8000");
        } else {
            driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), DesiredCapabilities.chrome());
            driver.get("http://frontend:8000");
        }
        groupEditor = new GroupEditor(driver);
        conceptTree = new ConceptTree(driver);
    }

    @Test
    public void dragAndDrop() {
        expandNode(conceptTree.getTreeNode("Movie Appearances", true));
        expandNode(conceptTree.getTreeNode("Awards", true));
        expandNode(conceptTree.getTreeNode("Academy Award", true));

        dNdNode(conceptTree.getTreeNode("Action Movies"), groupEditor.getGroupEditorInitialDropzone());
        dNdNode(conceptTree.getTreeNode("Best Picture"), groupEditor.getAndDropZone(1));

        List<WebElement> actionsMovies = groupEditor.getOrElements("Action Movies", 1);
        List<WebElement> bestPicture = groupEditor.getAndElements("Best Picture", 1);

        Assert.assertTrue(actionsMovies.size() == 1);
        Assert.assertTrue(bestPicture.size() == 1);
    }

    private void expandNode(WebElement node) {
        new WebDriverWait(driver, 10).until(ExpectedConditions.elementToBeClickable(node));
        node.click();
    }

    private void dNdNode(WebElement node, WebElement target) {
        DragDropHelper.dragAndDrop(driver, node, target);
    }

    @After
    public void tearDown() {
        driver.quit();
    }

}
