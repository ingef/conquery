package com.bakdata.conquery.test.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 *
 * @author Marcus Baitz
 */
public class DragDropHelper {

    private static String simulateHTML5DragAndDrop = null;
    private static String simulateEvent = null;

    static {
        String javaScriptEventSimulator = getFileAsString("js/createDragEvent.js");
        simulateHTML5DragAndDrop = getFileAsString("js/simulateHTML5DragAndDrop.js") + javaScriptEventSimulator;
        simulateEvent = getFileAsString("js/simulateEvent.js") + javaScriptEventSimulator;
    }

    public enum Position {
        Top_Left,
        Top,
        Top_Right,
        Left,
        Center,
        Right,
        Bottom_Left,
        Bottom,
        Bottom_Right
    }

    public static int getX(Position pos, int width) {
        if (Position.Top_Left.equals(pos) || Position.Left.equals(pos) || Position.Bottom_Left.equals(pos)) {
            return 1;
        } else if (Position.Top.equals(pos) || Position.Center.equals(pos) || Position.Bottom.equals(pos)) {
            return width / 2;
        } else if (Position.Top_Right.equals(pos) || Position.Right.equals(pos) || Position.Bottom_Right.equals(pos)) {
            return width - 1;
        } else {
            return 0;
        }
    }

    static int getY(Position pos, int height) {
        if (Position.Top_Left.equals(pos) || Position.Top.equals(pos) || Position.Top_Right.equals(pos)) {
            return 1;
        } else if (Position.Left.equals(pos) || Position.Center.equals(pos) || Position.Right.equals(pos)) {
            return height / 2;
        } else if (Position.Bottom_Left.equals(pos) || Position.Bottom.equals(pos) || Position.Bottom_Right.equals(pos)) {
            return height - 1;
        } else {
            return 0;
        }
    }

    /**
     * Calls a drag event
     *
     * @param driver The WebDriver to execute on
     * @param dragFrom The WebElement to simulate on
     * @param eventName The event name to call
     * @param clientX The mouse click X position on the screen
     * @param clientY The mouse click Y position on the screen
     * @param data The data transfer data
     * @return The updated data transfer data
     */
    public static Object simulateEvent(WebDriver driver, WebElement dragFrom, String eventName, int clientX, int clientY, Object data) {
        return ((JavascriptExecutor) driver).executeScript(simulateEvent, dragFrom, eventName, clientX, clientY, data);
    }

    private static String getFileAsString(String fileName) {
        try {
            ClassLoader classLoader = DragDropHelper.class.getClassLoader();
            File file = new File(classLoader.getResource(fileName).getFile());
            return FileUtils.readFileToString(file, Charset.defaultCharset());
        } catch (IOException ex) {
            Logger.getLogger(DragDropHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        throw new IllegalStateException("Can't load File: " + fileName);
    }

    /**
     * Calls a drag event
     *
     * @param driver The WebDriver to execute on
     * @param dragFrom The WebElement to simulate on
     * @param eventName The event name to call
     * @param mousePosition The mouse click area in the element
     * @param data The data transfer data
     * @return The updated data transfer data
     */
    public static Object simulateEvent(WebDriver driver, WebElement dragFrom, String eventName, Position mousePosition, Object data) {
        Point fromLocation = dragFrom.getLocation();
        Dimension fromSize = dragFrom.getSize();

        // Get Client X and Client Y locations
        int clientX = fromLocation.x + (fromSize == null ? 0 : getX(mousePosition, fromSize.width));
        int clientY = fromLocation.y + (fromSize == null ? 0 : getY(mousePosition, fromSize.height));

        return simulateEvent(driver, dragFrom, eventName, clientX, clientY, data);
    }

    /**
     * Drags and drops a web element from source to target
     *
     * @param driver The WebDriver to execute on
     * @param dragFrom The WebElement to drag from
     * @param dragTo The WebElement to drag to
     * @param dragFromX The position to click relative to the top-left-corner of
     * the client
     * @param dragFromY The position to click relative to the top-left-corner of
     * the client
     * @param dragToX The position to release relative to the top-left-corner of
     * the client
     * @param dragToY The position to release relative to the top-left-corner of
     * the client
     */
    public static void dragAndDrop(WebDriver driver, WebElement dragFrom, WebElement dragTo, int dragFromX, int dragFromY, int dragToX, int dragToY) {
        ((JavascriptExecutor) driver).executeScript(simulateHTML5DragAndDrop, dragFrom, dragTo, dragFromX, dragFromY, dragToX, dragToY);
    }

    /**
     * Drags and drops a web element from source to target Default
     * Position.Top_Right
     *
     * @param driver The WebDriver to execute on
     * @param dragFrom The WebElement to drag from
     * @param dragTo The WebElement to drag to
     */
    public static void dragAndDrop(WebDriver driver, WebElement dragFrom, WebElement dragTo) {
        Point fromLocation = dragFrom.getLocation();
        Point toLocation = dragTo.getLocation();
        Dimension fromSize = dragFrom.getSize();
        Dimension toSize = dragTo.getSize();

        // Get Client X and Client Y locations
        int dragFromX = fromLocation.x + (fromSize == null ? 0 : getX(Position.Top_Right, fromSize.width));
        int dragFromY = fromLocation.y + (fromSize == null ? 0 : getY(Position.Top_Right, fromSize.height));
        int dragToX = toLocation.x + (toSize == null ? 0 : getX(Position.Top_Right, toSize.width));
        int dragToY = toLocation.y + (toSize == null ? 0 : getY(Position.Top_Right, toSize.height));

        dragAndDrop(driver, dragFrom, dragTo, dragFromX, dragFromY, dragToX, dragToY);
    }

    /**
     * Drags and drops a web element from source to target
     *
     * @param driver The WebDriver to execute on
     * @param dragFrom The WebElement to drag from
     * @param dragTo The WebElement to drag to
     * @param dragFromPosition The place to click on the dragFrom
     * @param dragToPosition The place to release on the dragTo
     */
    public static void dragAndDrop(WebDriver driver, WebElement dragFrom, WebElement dragTo, Position dragFromPosition, Position dragToPosition) {
        Point fromLocation = dragFrom.getLocation();
        Point toLocation = dragTo.getLocation();
        Dimension fromSize = dragFrom.getSize();
        Dimension toSize = dragTo.getSize();

        // Get Client X and Client Y locations
        int dragFromX = fromLocation.x + (fromSize == null ? 0 : getX(dragFromPosition, fromSize.width));
        int dragFromY = fromLocation.y + (fromSize == null ? 0 : getY(dragFromPosition, fromSize.height));
        int dragToX = toLocation.x + (toSize == null ? 0 : getX(dragToPosition, toSize.width));
        int dragToY = toLocation.y + (toSize == null ? 0 : getY(dragToPosition, toSize.height));

        dragAndDrop(driver, dragFrom, dragTo, dragFromX, dragFromY, dragToX, dragToY);
    }

    //-------------
    // Cross-Window Drag And Drop Example
    //-------------
    public static void dragToWindow(WebDriver dragFromDriver, WebElement dragFromElement, WebDriver dragToDriver) {
        // Drag start
        simulateEvent(dragFromDriver, dragFromElement, "mousedown", Position.Center, null);
        Object dragData = simulateEvent(dragFromDriver, dragFromElement, "dragstart", Position.Center, null);
        dragData = simulateEvent(dragFromDriver, dragFromElement, "dragenter", Position.Center, dragData);
        dragData = simulateEvent(dragFromDriver, dragFromElement, "dragleave", Position.Left, dragData);
        dragData = simulateEvent(dragFromDriver, dragFromDriver.findElement(By.tagName("body")), "dragleave", Position.Left, dragData);

        // Drag to other window
        simulateEvent(dragToDriver, dragToDriver.findElement(By.tagName("body")), "dragenter", Position.Right, null);
        WebElement dropOverlay = dragToDriver.findElement(By.className("DropOverlay"));
        simulateEvent(dragToDriver, dropOverlay, "dragenter", Position.Right, null);
        simulateEvent(dragToDriver, dropOverlay, "dragover", Position.Center, null);
        dragData = simulateEvent(dragToDriver, dropOverlay, "drop", Position.Center, dragData);
        simulateEvent(dragFromDriver, dragFromElement, "dragend", Position.Center, dragData);
    }

}
