package com.gbr.integrationsys.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface XmlParseService {
    List<String> parseXml(InputStream in, Map config) throws Exception;
    void doInsertData(InputStream in, Map config, String site) throws Exception;
    void doTest(String site);
}
