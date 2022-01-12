package testpackage.softassert;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.steps.StepEventBus;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import testpackage.pages.MainPage;
import testpackage.pdf.Pdf;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SerenitySoftAssert extends SoftAssertions {

    MainPage page;

    private static ConcurrentHashMap<Long, List<SoftAssertError>> errors = new ConcurrentHashMap<>(); //содержит все ошибки переданные в метод assertSoft всеми потоками. Ключом являе id потока.

    int errorCount = 0; //счетчик ошибок


    /**
     * Вызов метода вызывает прикрепелние к отчету спика всех найденных ошибок для потока и падению теста, если такие ошибки существуют
     */

    public void assertAll(MainPage pageObject) {
        this.page = pageObject;
        StringBuilder sb = new StringBuilder();
        List<SoftAssertError> softErrors = errors.getOrDefault(Thread.currentThread()
                                                                     .getId(), new ArrayList<>());
        if (softErrors.size() > 0) {
            for (SoftAssertError error : softErrors) {
                sb.append("&#13;&#10;")
                  .append("<br>");
                sb.append("=========================================================")
                  .append("&#13;&#10;")
                  .append("<br>");
                Pattern pattern = Pattern.compile("(!!! .* !!!)");
                Matcher matcher = pattern.matcher(error.getStep());
                if (matcher.find()) {
                    sb.append(matcher.group(0));
                }
                sb.append("&#13;&#10;")
                  .append("<br>");
                sb.append(error.getAssertionError()
                               .getMessage());
                sb.append("&#13;&#10;")
                  .append("<br>");
                sb.append("=========================================================")
                  .append("&#13;&#10;")
                  .append("<br>");
                sb.append("&#13;&#10;")
                  .append("<br>");
            }
            page.getDriver()
                .quit();
            StepEventBus.getEventBus()
                        .testFailed(new AssertionError(sb.toString()));
            Assert.fail("SoftAssert failed");
        }
    }

    /**
     * Метод принимает AbstractAssert в качестве параметра и если проверка провалена добавляет ошибку
     * в список ошибок текущего потока, изменяет имя шага отмечая его как проваленный, и добавляет в шаг сообщение об ошибке
     */

    public void assertSoft(AbstractAssert assert1) {
        List<SoftAssertError> softErrors = errors.getOrDefault(Thread.currentThread()
                                                                     .getId(), new ArrayList<>());
        List<Throwable> currentList = this.errorsCollected();
        if (currentList.size() > 0 && errorCount != this.errorsCollected()
                                                        .size()) {
            String stepName = StepEventBus.getEventBus()
                                          .getCurrentStep()
                                          .toString()
                                          .replaceAll("^Optional\\[", "")
                                          .replaceAll("]$", "");
            StringBuilder allErrorMessages = new StringBuilder();
            for (Throwable error : currentList.subList(errorCount, currentList.size())) {
                String shortStepName = stepName;
                Pattern pattern = Pattern.compile("!!! (.*) !!!");
                Matcher matcher = pattern.matcher(stepName);
                if (matcher.find()) {
                    shortStepName = matcher.group(0);
                }
                SoftAssertError newError = new SoftAssertError(error,
                                                               "!!! " + shortStepName + " !!!");
                softErrors.add(newError);
                allErrorMessages.append(error.getMessage())
                                .append("&#13;&#10;\n\r")
                                .append("&#13;&#10;\n\r");
            }
            System.out.println(errorCount + "  " + currentList.size());
            errorCount = currentList.size();
            errors.replace(Thread.currentThread()
                                 .getId(), softErrors);
            StepEventBus eventBus = StepEventBus.getEventBus();
            StringBuilder sb = new StringBuilder();
            if (!stepName.contains("=Assertion error=")) {
                sb.append("&#13;&#10;\n\r");
                sb.append("!!! " + stepName + " !!!&#13;&#10;\n\r");
                sb.append("=================Assertion error=====================&#13;&#10;\n\r");
                sb.append(allErrorMessages)
                  .append("&#13;&#10;\n\r");
                sb.append("===============================================&#13;&#10;&#13;&#10;\n\r\n\r");
            } else {
                StringBuilder nexError = new StringBuilder();
                nexError.append("&#13;&#10;\n\r" + allErrorMessages + "&#13;&#10;\n\r");
                nexError.append("===============================================");
                sb.append(stepName.replaceAll("===============================================", nexError.toString()));
            }
            eventBus.updateCurrentStepTitle(sb.toString());
            eventBus.takeScreenshot();
            page.scrollToUp();
            eventBus.takeScreenshot();
            page.scrollToDown();
            eventBus.takeScreenshot();
            StepEventBus.overrideEventBusWith(eventBus);
        }
    }

    /**
     * Метод принимает AbstractAssert в качестве параметра и если проверка провалена добавляет ошибку
     * в список ошибок текущего потока, изменяет имя шага отмечая его как проваленный, добавляет в шаг
     * сообщение об ошибке и прикрепляет к шагу PDF файл и результат его перевода в текст.
     */

    public void assertPdfSoft(AbstractAssert assert1) {
        List<SoftAssertError> softErrors = errors.getOrDefault(Thread.currentThread()
                                                                     .getId(), new ArrayList<>());
        List<Throwable> currentList = this.errorsCollected();
        if (currentList.size() > 0 && errorCount != this.errorsCollected()
                                                        .size()) {
            String stepName = StepEventBus.getEventBus()
                                          .getCurrentStep()
                                          .toString()
                                          .replaceAll("^Optional\\[", "")
                                          .replaceAll("]$", "");
            StringBuilder allErrorMessages = new StringBuilder();
            for (Throwable error : currentList.subList(errorCount, currentList.size())) {
                String shortStepName = stepName;
                Pattern pattern = Pattern.compile("!!! (.*) !!!");
                Matcher matcher = pattern.matcher(stepName);
                if (matcher.find()) {
                    shortStepName = matcher.group(0);
                }
                SoftAssertError newError = new SoftAssertError(error,
                                                               "!!! " + shortStepName + " !!!");
                softErrors.add(newError);
                allErrorMessages.append(error.getMessage())
                                .append("&#13;&#10;\n\r")
                                .append("&#13;&#10;\n\r");
            }
            try {
                Serenity.recordReportData()
                        .asEvidence()
                        .withTitle("PDF")
                        .downloadable()
                        .fromFile(Pdf.getPdfPath());
                Serenity.recordReportData()
                        .asEvidence()
                        .withTitle("PDF_AS_TEXT")
                        .downloadable()
                        .fromFile(makeFile(Pdf.getParseResult()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(errorCount + "  " + currentList.size());
            errorCount = currentList.size();
            errors.replace(Thread.currentThread()
                                 .getId(), softErrors);
            StepEventBus eventBus = StepEventBus.getEventBus();
            StringBuilder sb = new StringBuilder();
            if (!stepName.contains("=Assertion error=")) {
                sb.append("&#13;&#10;\n\r");
                sb.append("!!! " + stepName + " !!!&#13;&#10;\n\r");
                sb.append("=================Assertion error=====================&#13;&#10;\n\r");
                sb.append(allErrorMessages)
                  .append("&#13;&#10;\n\r");
                sb.append("===============================================&#13;&#10;&#13;&#10;\n\r\n\r");
            } else {
                StringBuilder nexError = new StringBuilder();
                nexError.append("&#13;&#10;\n\r" + allErrorMessages + "&#13;&#10;\n\r");
                nexError.append("===============================================");
                sb.append(stepName.replaceAll("===============================================", nexError.toString()));
            }
            eventBus.updateCurrentStepTitle(sb.toString());
            eventBus.takeScreenshot();
            page.scrollToUp();
            eventBus.takeScreenshot();
            page.scrollToDown();
            eventBus.takeScreenshot();
            StepEventBus.overrideEventBusWith(eventBus);
        }
    }

    /**
     * Метод принимает AbstractAssert и PageObject в качестве параметра. Если проверка провалена добавляет ошибку
     * в список ошибок текущего потока, изменяет имя шага отмечая его как проваленный, и добавляет в шаг сообщение об ошибке
     * Метод предназначен для использования SerenitySoftAssert вне классов шагов
     */

    public void assertSoft(MainPage page, AbstractAssert assert1) {
        this.page = page;
        assertSoft(assert1);
    }


    /**
     * Очищает список ошибок для текущего потока
     */

    public void resetErrors() {
        errors.put(Thread.currentThread()
                         .getId(), new ArrayList<>());
    }

    /**
     * Создает HTML файл с указанным содержимым для прикрепления его к отчету
     */
    private Path makeFile(String content) {
        String patternPath = "src/test/java/testpackage/softassert/assertError.html";
        StringBuilder patternFile = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(patternPath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> patternFile.append(s)
                                           .append("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        File resultFile = new File("target/" + Thread.currentThread()
                                                     .getId() + ".html");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultFile), StandardCharsets.UTF_8))) {
            writer.write(patternFile.toString()
                                    .replace("%error", content.replaceAll("\n", "<br>")));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return resultFile.toPath();
    }
}
