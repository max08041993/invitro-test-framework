package ru.invitro.testing.basis.framework.pdf;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import net.serenitybdd.core.Serenity;
import net.serenitybdd.core.pages.PageObject;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;
import ru.invitro.testing.basis.framework.page.ChromeDownloadPage;
import ru.invitro.testing.basis.framework.savedata.SavedData;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class Pdf extends PageObject {

    private static final ConcurrentHashMap<Long, String> parseResults = new ConcurrentHashMap<>(); //результат обработки PDF для каждого потока

    private static final ConcurrentHashMap<Long, Path> pdfFiles = new ConcurrentHashMap<>(); //путь к последнему прочитанному PDF для каждого потока

    ChromeDownloadPage chromeDownloadPage;
    String chromeDownloadPath = "C:\\chrome\\";

    public void setChromeDownloadPath(String chromeDownloadPath) {
        this.chromeDownloadPath = chromeDownloadPath;
    }

    /**
     * Метод возвращает путь к последнему прочитанному PDF файлу для текущего потока
     *
     * @return путь к последнему прочитанному PDF файлу для текущего потока
     */
    public static Path getPdfPath() {
        return pdfFiles.get(Thread.currentThread()
                                  .getId());
    }

    /**
     * Метод сохраняет путь к последнему прочитанному PDF файлу для текущего потока
     */
    public void setPdfPath(Path file) {
        pdfFiles.put(Thread.currentThread()
                           .getId(), file);
    }

    /**
     * Метод возвращает результат обработки последнего прочитанного PDF файла для текущего потока
     *
     * @return результат обработки последнего прочитанного PDF файла для текущего потока
     */
    public static String getParseResult() {
        return parseResults.get(Thread.currentThread()
                                      .getId());
    }

    /**
     * Метод сохраняет результат обработки последнего прочитанного PDF файла для текущего потока
     */
    public void setParseResult(String parseResult) {
        parseResults.put(Thread.currentThread()
                               .getId(), parseResult);
    }

    /**
     * Метод скачивает и считывает PDF файл.
     *
     * @param useTikaParser Параметр опрделеяет какой парсер будет использоваться
     *                      true - текст будет сгруппирован логическими блоками
     *                      false - порядок строк текста будет зависеть от Y координат строки на листе
     */
    public void readPdf(boolean useTikaParser) {
        downloadPdf();
        parsePdf(useTikaParser);
    }

    public abstract void downloadPdf();


    /**
     * Считывает последний скаченный PDF файл.
     *
     * @param useTikaParser Параметр опрделеяет какой парсер будет использоваться
     *                      true - текст будет сгруппирован логическими блоками
     *                      false - порядок строк текста будет зависеть от Y координат строки на листе
     */
    public void parsePdf(boolean useTikaParser) {
        String filename = chromeDownloadPage.getLastDowloadedFile();
        String fullFilename = chromeDownloadPath + filename;
        setPdfPath(new File(fullFilename).toPath());
        try {
            Serenity.recordReportData()
                    .asEvidence()
                    .withTitle("PDF")
                    .downloadable()
                    .fromFile(getPdfPath()); //Прикрепляет ПДФ к отчету
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(fullFilename);
        try {
            if (useTikaParser) {
                BodyContentHandler handler = new BodyContentHandler();
                Metadata metadata = new Metadata();
                ParseContext context = new ParseContext();
                PDFParser pdfParser = new PDFParser();

                PDFParserConfig config = pdfParser.getPDFParserConfig();
                config.setSortByPosition(true);
                pdfParser.setPDFParserConfig(config);
                File initialFile = new File(fullFilename);
                InputStream targetStream = new FileInputStream(initialFile);
                pdfParser.parse(targetStream, handler, metadata, context);

                setParseResult(handler.toString());
                Pattern endPagePattern = Pattern.compile("(ARMPS-MO-\\d+\\.\\d+\\.\\d+\\(.*\\))");
                Matcher matcher = endPagePattern.matcher(getParseResult());
                if (matcher.find()) {
                    setParseResult(getParseResult().replace(matcher.group(1),
                                                            matcher.group(1)
                                                                    + " ==--==\r\n")); //Добавление метки окончания страницы
                }
                setParseResult(getParseResult().replace((char) 946, (char) 63)
                                               .replace((char) 945, (char) 63)
                                               .replace((char) 8722, '-')); // Нормализация текста. Замена спецсимволов
            } else {
                PdfReader reader = new PdfReader(fullFilename);
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= reader.getNumberOfPages(); ++i) {
                    TextExtractionStrategy strategy = new SimpleTextExtractionStrategy();
                    String text = PdfTextExtractor.getTextFromPage(reader, i, strategy);
                    sb.append(text)
                      .append(" ==--==\r\n"); //Добавление метки окончания страницы
                }

                reader.close();
                setParseResult(sb.toString());
            }
        } catch (IOException | SAXException | TikaException e) {
            e.printStackTrace();
        }
    }


    /**
     * Разбивает результат чтения PDF на бланки
     *
     * @return лист с бланками документа
     */
    private List<String> splitByBlank() {
        return Arrays.asList(getParseResult().split("==--==[ \\r\\n]*(Лицензия № [^ \\r\\n]+)?[^а-яА-Яa-zA-Z\\d]*(Общество с ограниченной ответственностью|test|ООО )"));
    }

    /**
     * Разбивает результат чтения PDF на страницы
     *
     * @return лист со страницами документа
     */
    private List<String> splitByPage() {
        return Arrays.asList(getParseResult().split(" ==--=="));
    }

    /**
     * Метод проверяет что указанные строки находятся на одном бланке
     *
     * @param stringsForSearch Лист строк для поиска
     * @return true - если найден бланк, содержащий все указанные строки
     */
    public boolean containsOnOneBlank(List<String> stringsForSearch) {
        return splitByBlank().stream()
                             .anyMatch(blank -> stringsForSearch.stream()
                                                                .allMatch(string -> contains(string, blank)));
    }

    /**
     * Преобразовывает строку: заменяет параметры на необходимые данные и превращает строку в регулярное выражение
     * %текущая_дата - заменяется текущей датой в формате dd.MM.yyyy
     * %saved_data - заменяется переменной хранящейся в SavedData.getForPDF()
     * %ignore - заменяется регулярным выражением "[^ ]+"
     *
     * @param sourceString строка для преобразования
     * @return String результат преобразования
     */
    public String createRegex(String sourceString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        String nowData = dateFormat.format(new Date());
        sourceString = normaliseText(sourceString).replaceAll("%текущая_дата", nowData);
        if (sourceString.contains("%saved_data")) {
            sourceString = sourceString.replaceAll("%saved_data", SavedData.getStatic("ForPDF"));
        }
        sourceString = Pattern.quote(sourceString);
        return sourceString.replaceAll("%ignore", "\\\\E[^ ]+\\\\Q")
                           .replaceAll("\\\\Q\\\\E", "");

    }

    /**
     * Преобразует коллекцию строк в регулярное выражение для поиска. Каждая строка преобразуется методом createRegex(String sourceString)
     * а затем строки объединяются регулярным выражением " .*"
     *
     * @param sourceStrings Коллекция строк для преобразования
     * @return String результат преобразования
     */
    public String createRegex(List<String> sourceStrings) {
        return sourceStrings.stream()
                            .map(this::createRegex)
                            .collect(Collectors.joining(" .*"));
    }

    /**
     * Преобразует текст: заменяет переносы пробелами и убирает множественные пробелы
     *
     * @param text Текст для пробразования
     * @return String результат преобразования
     */
    public String normaliseText(String text) {
        return text.replaceAll("[ \r\n]+", " ")
                   .replaceAll(" {2,}", " ")
                   .trim();
    }

    /**
     * Метод проверяет что указанная строка присутствует в тексте
     *
     * @param searchString строка для поиска
     * @param source       текст для анализа
     * @return true - если строка найдена в тексте
     */
    public boolean contains(String searchString, String source) {
        Pattern pattern = Pattern.compile(createRegex(searchString));
        Matcher matcher = pattern.matcher(normaliseText(source));
        System.out.println(pattern);
        return matcher.find();
    }

    /**
     * Метод проверяет что указанная строка присутствует в PDF файле
     *
     * @param string строка для поиска
     * @return true - если строка найдена в PDF файле
     */
    public boolean contains(String string) {
        return contains(string, getParseResult());
    }

    /**
     * Метод проверяет что указанные строки присутствую в тексте в указанном порядке
     *
     * @param searchStrings Коллекция строк
     * @param source        текст для анализа
     * @return true - если строки найдены в тексте в указанном порядке
     */
    public boolean containsInOrder(List<String> searchStrings, String source) {
        System.out.println(createRegex(searchStrings));
        Pattern pattern = Pattern.compile(createRegex(searchStrings));
        Matcher matcher = pattern.matcher(normaliseText(source));
        return matcher.find();
    }

    /**
     * Метод проверяет что указанные строки присутствую в PDF файле в указанном порядке
     *
     * @param searchStrings Коллекция строк
     * @return true - если строки найдены в PDF файле в указанном порядке
     */
    public boolean containsInOrder(List<String> searchStrings) {
        return containsInOrder(searchStrings, getParseResult());
    }

    /**
     * Метод проверяет что указанные строки присутствую в тексте
     *
     * @param searchStrings Коллекция строк
     * @param source        текст для анализа
     * @return true - если строки найдены в тексте
     */
    public boolean containsAll(List<String> searchStrings, String source) {
        return searchStrings.stream()
                            .allMatch(string -> contains(string, source));
    }


    /**
     * Метод проверяет что указанные строки присутствую в PDF файле
     *
     * @param searchStrings Коллекция строк
     * @return true - если строки найдены в PDF файле
     */
    public boolean containsAll(List<String> searchStrings) {
        return searchStrings.stream()
                            .allMatch(string -> contains(string, getParseResult()));
    }

    /**
     * Метод извлекает список ИНЗ из PDF файла
     *
     * @return List(String) список ИНЗ
     */
    public List<String> getInz() {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("ИНЗ: *([\\d]+) *");
        Matcher matcher = pattern.matcher(getParseResult());
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    /**
     * Метод извлекает список первое ИНЗ из текста
     *
     * @return String ИНЗ или "" если ИНЗ не найдено
     */
    public String getFirstInzFrom(String source) {
        Pattern pattern = Pattern.compile("ИНЗ: *([\\d]+) *");
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * Метод извлекает список ИНЛ из PDF файла
     *
     * @return List(String) список ИНЛ
     */
    public List<String> getIln() {
        List<String> result = new ArrayList<>();
        for (String inz : getInz()) {
            Pattern pattern = Pattern.compile("(" + inz + "\\d+) [А-Я ]+\\d+ *.*");
            Matcher matcher = pattern.matcher(getParseResult());
            while (matcher.find()) {
                result.add(matcher.group(1));
            }
        }
        return result;
    }

    /**
     * Метод извлекает тип и код пробирок
     *
     * @return Map(String, String) где key - это код пробирки, а value - тип
     */
    public Map<String, String> getTubeType() {
        Map<String, String> tubes = new HashMap<>();
        for (String inl : getIln()) {
            Pattern pattern = Pattern.compile(inl + " ([А-Я]+\\d+) ([А-Я/\\-]+) \\d+ *.*");
            Matcher matcher = pattern.matcher(getParseResult());
            if (matcher.find()) {
                tubes.put(matcher.group(1), matcher.group(2));
            }
        }
        return tubes;
    }

    /**
     * Метод возвращает количество бланков, на которых есть все указанные строки
     *
     * @param stringsForSearch Список строк для поиска
     * @return Long количество бланков, на которых есть все указанные строки
     */
    public Long numberOfBlanksContainsStrings(List<String> stringsForSearch) {
        return splitByBlank().stream()
                             .filter(blank -> containsAll(stringsForSearch, blank))
                             .count();
    }

    /**
     * Метод возвращает количество страниц, на которых есть все указанные строки
     *
     * @param stringsForSearch Список строк для поиска
     * @return Long количество страниц, на которых есть все указанные строки
     */
    public Long numberOfPagesContainsStrings(List<String> stringsForSearch) {
        return splitByPage().stream()
                            .filter(blank -> containsAll(stringsForSearch, blank))
                            .count();
    }

    /**
     * Метод возвращает бланки, на которых есть все указанные строки
     *
     * @param stringsForSearch Список строк для поиска
     * @return List(String) бланки, на которых есть все указанные строки
     */
    public List<String> blanksWithStrings(List<String> stringsForSearch) {
        return splitByBlank().stream()
                             .filter(blank -> containsAll(stringsForSearch, blank))
                             .collect(Collectors.toList());
    }

    /**
     * Метод возвращает количество включений, строки в тексте
     *
     * @param searchString Строка для поиска
     * @return Integer количество включений, строки в тексте
     */
    public Integer numberOfInclusionsString(String searchString, String source) {
        Pattern pattern = Pattern.compile(createRegex(searchString));
        Matcher matcher = pattern.matcher(normaliseText(source));
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Метод извлекает сумму сметы из PDF файла
     *
     * @return String сумма сметы
     */
    public String getSmetaOrderSum() {
        Scanner scanner = new Scanner(getParseResult());
        Pattern sumPattern = Pattern.compile("\\d+.\\d+ \\d+.\\d+ (\\d+).\\d+");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains("Всего по смете:")) {
                String nextLine = scanner.nextLine();
                Matcher matcher = sumPattern.matcher(nextLine);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        scanner.close();
        return "сумма не найдена";
    }

    /**
     * Метод извлекает номера контейнеров из PDF файла
     *
     * @return List(String) номера контейнеров
     */
    public List<String> getContainers() {
        List<String> result = new ArrayList<>();
        Pattern containersPattern = Pattern.compile("\\d{12,} [^\\n ]+ (.*) (\\d+) ([^\\n]*)\\n");
        Matcher matcher = containersPattern.matcher(getParseResult());
        while (matcher.find()) {
            int containerAmount = Integer.parseInt(matcher.group(2));
            for (int i = 0; i < containerAmount; i++) {
                result.add(matcher.group(1));
            }
        }
        return result;
    }

    /**
     * Метод извлекает ИНЛ и количество контейнеров
     *
     * @return Map(String, String) где key - это ИНЛ, а value - количество контейнеров
     */
    public Map<String, String> getInlAndContainers() {
        Map<String, String> result = new HashMap<>();
        Pattern containersPattern = Pattern.compile("(\\d{12,}) [^\\n ]+ (.*) (\\d+) ([^\\n]*)\\n");
        Matcher matcher = containersPattern.matcher(getParseResult());
        while (matcher.find()) {
            String inl = matcher.group(1);
            String containerAmount = matcher.group(3);
            result.put(inl, containerAmount);
        }
        return result;
    }


    /**
     * @return количество бланков в PDF
     */
    public Integer getNumberOfBlanks() {
        return splitByBlank().size();
    }

    /**
     * @return количество страниц в PDF
     */

    public Integer getNumberOfPages() {
        return splitByPage().size() - 1;
    }

    /**
     * Метод извлекает ИНЗ по тесту
     *
     * @param tests - список тестов
     * @return Map(String, List (String)) где key - это название теста, а value - список ИНЗ этого теста
     */
    public Map<String, List<String>> getInzByTests(List<String> tests) {
        Map<String, List<String>> result = new HashMap<>();
        List<String> inzBlocks = new ArrayList<>(Arrays.asList(getParseResult().replaceAll("ИНЗ:", "inzMarkerИНЗ:")
                                                                               .split("inzMarker")));
        tests.forEach(test -> result.put(test, inzBlocks.stream()
                                                        .filter(inzBlock -> contains(test, inzBlock))
                                                        .map(this::getFirstInzFrom)
                                                        .collect(Collectors.toList())));
        return result;
    }

    /**
     * Метод извлекает дату готовности продуктов из бланк-сметы
     *
     * @return Map(String, LocalDateTime) где key - продукт, а value - LocalDateTime дата готовности
     */
    public Map<String, LocalDateTime> getSmetaResultDates() {
        Map<String, LocalDateTime> result = new HashMap<>();
        List<String> sourceByString = new ArrayList<>(Arrays.asList(getParseResult().split("[\r\n]+")));
        Pattern resultDatePattern = Pattern.compile("^([^ \\r\\n]+) .+ до (\\d{2}:\\d{2} \\d{2}\\.\\d{2}\\.\\d{4}).*$");
        Locale en = new Locale("en", "EN");
        DateTimeFormatter formatToDate = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy", en);
        for (String string : sourceByString) {
            Matcher matcher = resultDatePattern.matcher(string);
            if (matcher.find()) {
                String productCode = matcher.group(1);
                LocalDateTime resultDate = LocalDateTime.parse(matcher.group(2), formatToDate);
                result.put(productCode, resultDate);
            }
        }
        return result;
    }

    /**
     * Метод извлекает список ИНЗ из бланк-сметы
     *
     * @return Set(String) список ИНЗ
     */
    public Set<String> getSmetaInzs() {
        Set<String> result = new HashSet<>();
        Pattern pattern = Pattern.compile("ИНЗ: ([\\d,]*)");
        Matcher matcher = pattern.matcher(getParseResult());
        while (matcher.find()) {
            result.addAll(new HashSet<>(Arrays.asList(matcher.group(1)
                                                             .split(","))));
        }
        return result;
    }
}
