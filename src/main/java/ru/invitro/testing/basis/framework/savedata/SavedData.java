package ru.invitro.testing.basis.framework.savedata;

import org.assertj.core.api.Assertions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SavedData {

    final private HashMap<String, Object> saved = new HashMap<>();

    final private static ConcurrentHashMap<Long, SavedData> staticSaved = new ConcurrentHashMap<>();


    /**
     * Добавляет в статическую переменную данные пару ключ - значение
     * используется для передачи данных между методами вне step-классов где не работает аннотация @Shared
     *
     * @param key   String уникальный ключ
     * @param value Object сохраняемое значение
     */

    public static void putStatic(String key, Object value) {
        long threadId = Thread.currentThread()
                              .getId();
        if (!staticSaved.containsKey(threadId)) {
            staticSaved.put(threadId, new SavedData());
        }
        staticSaved.get(threadId)
                   .put(key, value);
    }

    /**
     * Возвращает данные указанного типа по ключу из статической переменной
     * используется для передачи данных между методами вне step-классов где не работает аннотация @Shared
     *
     * @param key    String ключ
     * @param <Type> Класс значения
     * @return Значение приведенное к указанному классу
     */
    public static <Type> Type getStatic(String key) {
        long threadId = Thread.currentThread()
                              .getId();
        Assertions.assertThat(staticSaved)
                  .as("Нет сохранённых данных для потока с указанным ID")
                  .containsKey(threadId);
        return staticSaved.get(threadId)
                          .get(key);
    }

    /**
     * Добавляет в сохраненные данные пару ключ - значение
     *
     * @param key   String уникальный ключ
     * @param value Object сохраняемое значение
     */

    public void put(String key, Object value) {
        saved.put(key, value);
    }

    /**
     * Добавляет в сохраненные данные все пары ключ - значение из Map
     *
     * @param data Map(String, ?) коллекция пар ключ - значение для сохранения
     */

    public void put(Map<String, ?> data) {
        saved.putAll(data);
    }

    /**
     * Возвращает данные указанного типа по ключу из сохраненных данных
     *
     * @param key    String ключ
     * @param <Type> Класс значения
     * @return Значение приведенное к указанному классу
     */

    public <Type> Type get(String key) {
        Assertions.assertThat(saved)
                  .as("Нет сохранённых данных по ключу")
                  .containsKey(key);
        return (Type) saved.get(key);
    }

    /**
     * Проверяет наличие ключа в сохраненных данных
     *
     * @param key String ключ
     * @return true - если ключ присутствует в сохраненных данных
     */

    public boolean containsKey(String key) {
        return saved.containsKey(key);
    }

    /**
     * Проверяет наличие ключа в статической переменной
     * используется для передачи данных между методами вне step-классов где не работает аннотация @Shared
     *
     * @param key String ключ
     * @return true - если ключ присутствует в сохраненных данных
     */
    public boolean containsStaticKey(String key) {
        long threadId = Thread.currentThread()
                              .getId();
        return staticSaved.containsKey(threadId) && staticSaved.get(threadId)
                                                               .containsKey(key);
    }

}

