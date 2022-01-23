package ru.invitro.testing.basis.framework.page;

import org.junit.Assert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

public class ChromeDownloadPage extends BasePage {

    /**
     * Метод открывает новую вкладку браузера
     * переключается на нее
     * открывает страницу загрузок браузера
     * и возвращает имя последнего сохраненного файла
     *
     * @return имя последнего сохраненного файла (String)
     */
    public String getLastDowloadedFile() {
        WebDriver driver = getDriver();
        String currentWindow = driver.getWindowHandle();
        ((JavascriptExecutor) driver).executeScript("window.open();");
        for (String tab : driver.getWindowHandles()) {
            if (!tab.equals(currentWindow)) {
                driver.switchTo()
                      .window(tab);
                break;
            }
        }
        driver.navigate()
              .to("chrome://downloads/");
        waitABit(2000);
        String fileName = "";
        int count = 0;
        while (count < 3) {
            try {
                Object fileNameFromCromeDownloads = ((JavascriptExecutor) getDriver()).executeScript("return document.querySelector('downloads-manager').shadowRoot.querySelector('downloads-item').shadowRoot.querySelector('#file-link').text");
                fileName = fileNameFromCromeDownloads.toString();
                break;
            } catch (WebDriverException e) {
                waitABit(60000);
                getDriver().navigate()
                           .refresh();
                waitABit(10000);
                count++;
            }
        }
        if (count == 3) {
            Assert.fail("Файл не загрузился");
        }
        driver.close();
        driver.switchTo()
              .window(currentWindow);
        return (fileName);
    }
}
