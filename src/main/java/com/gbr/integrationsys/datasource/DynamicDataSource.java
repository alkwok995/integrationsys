package com.gbr.integrationsys.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {
    private static final Logger log = LoggerFactory.getLogger(DynamicDataSource.class);

    @Override
    protected Object determineCurrentLookupKey() {
        Object o = DynamicDataSourceHolder.getDataSource();
        //log.info("current thread " + Thread.currentThread().getName() + " add " + o + " to ThreadLocal");
        return o;
    }
}
