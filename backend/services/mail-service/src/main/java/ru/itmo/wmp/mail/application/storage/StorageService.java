package ru.itmo.wmp.mail.application.storage;

import java.io.InputStream;

public interface StorageService {
    String upload(String filename, InputStream inputStream, long size, String contentType);

    InputStream download(String storageKey);

    void delete(String storageKey);
}
