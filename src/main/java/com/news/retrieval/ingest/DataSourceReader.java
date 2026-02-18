package com.news.retrieval.ingest;

import java.io.InputStream;


public interface DataSourceReader {

    InputStream read();

    String sourceName();
}
