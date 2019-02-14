package com.gbr.integrationsys.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicDataSourceHolder {
    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceHolder.class);

    private static final ThreadLocal<String> DATA_SOURCE = new ThreadLocal<>();

    public static void putDataSource(String site) {
        log.debug("current thread " + Thread.currentThread().getName() + " add " + site + " to ThreadLocal");
        DATA_SOURCE.set(site);
    }

    public static String getDataSource() {
        return DATA_SOURCE.get();
    }

    public static void removeDataSource() {
        DATA_SOURCE.remove();
    }
}
