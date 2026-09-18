package br.com.immersionhub.generator.desktop.source;

import java.nio.file.Path;

public interface SourceDownloader {
    SourceDescriptor inspect(String canonicalUrl) throws Exception;
    Path download(String canonicalUrl, Path targetDirectory) throws Exception;
}
