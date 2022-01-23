package ru.invitro.testing.basis.framework.page;

import net.serenitybdd.core.pages.PageObject;
import org.openqa.selenium.JavascriptExecutor;

public class BasePage  extends PageObject {
    /**
     * Метод прокручивает до верха страницы
     */
    public void scrollToUp() {
        ((JavascriptExecutor) getDriver()).executeScript("window.scrollTo(0, 0)");
    }

    /**
     * Метод прокручивает до низа страницы
     */
    public void scrollToDown() {
        ((JavascriptExecutor) getDriver()).executeScript("window.scrollTo(0, document.body.scrollHeight)");
    }

}
