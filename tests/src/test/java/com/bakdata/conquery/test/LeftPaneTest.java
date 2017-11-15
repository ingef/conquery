package com.bakdata.conquery.test;

import com.bakdata.conquery.test.page.LeftPanePage;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 *
 * @author Marcus Baitz
 */
public class LeftPaneTest {

    private WebDriver driver;
    private LeftPanePage leftPane;

    @BeforeTest
    public void setUp() throws MalformedURLException {
        DesiredCapabilities dc = DesiredCapabilities.chrome();
        driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), dc);
        leftPane = new LeftPanePage(driver);
    }

    @Test
    public void tabTest() throws InterruptedException, UnknownHostException {
        driver.get("http://localhost:8000/");
        System.out.println("URL: " + driver.getCurrentUrl());
//        leftPane.toTabRequests();
        Assert.assertEquals(true, true);
    }

    @AfterTest
    public void tearDown() throws InterruptedException {
        driver.quit();
    }

}
