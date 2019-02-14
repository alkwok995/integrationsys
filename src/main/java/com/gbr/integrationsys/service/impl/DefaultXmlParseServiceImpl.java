package com.gbr.integrationsys.service.impl;

import com.gbr.integrationsys.exception.ImportException;
import com.gbr.integrationsys.service.XmlParseService;
import com.gbr.integrationsys.util.ElementUtil;
import com.gbr.integrationsys.util.ExpressionUtil;
import com.gbr.integrationsys.util.YamlUtil;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DefaultXmlParseServiceImpl implements XmlParseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultXmlParseServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<String> parseXml(InputStream in, Map config) throws Exception {
        SAXReader reader = new SAXReader();
        Document document = reader.read(new BufferedReader(new InputStreamReader(in, "UTF-8")));
        Element importElement = document.getRootElement().element("IMPORT");
        if (importElement == null) {
            throw new ImportException("XML解析错误：XML中没有找到IMPORT标签");
        }
        String importMode = importElement.attributeValue("mode");
        Element boElement = ElementUtil.getFirstChildElement(importElement);
        if (boElement == null) {
            throw new ImportException("XML解析错误：XML中没有找到基本对象标签");
        }

        //解析每行数据
        return parseBoTag(boElement, config, "");
    }

    private List<String> parseBoTag(Element boElement, Map params, String parentTagName) throws Exception{
        Iterator<Element> it = boElement.elementIterator();
        String tagName = parentTagName.equals("") ? boElement.getName().toLowerCase() :
                parentTagName + "." + boElement.getName().toLowerCase();
        String refsql = (String) YamlUtil.getValueByYamlKey(params, "tags." + tagName + ".refsql");
        String sqlTemplate = (String) YamlUtil.getValueByYamlKey(params, "mysql.insert.sql." + refsql);

        List<String> sqls = new ArrayList<>();
        while (it.hasNext()) {
            sqls.addAll(parseRow(it.next(), params, sqlTemplate, tagName));
        }

        return sqls;
    }

    private List<String> parseRow(Element rowElement, Map params, String sqlTemplate, String parentTagName) throws Exception{
        String s = sqlTemplate;
        Iterator<Element> it = rowElement.elementIterator();
        List<Element> subElement = new LinkedList<>();
        Map sqlParams = new LinkedHashMap<>();
        List<String> sqls = new ArrayList<>();

        while (it.hasNext()) {
            Element singleItem = it.next();
            String tagName = parentTagName.equals("") ? singleItem.getName().toLowerCase() :
                    parentTagName + "." + singleItem.getName().toLowerCase();
            String dbType,tagValue;

            String tagType = (String) YamlUtil.getValueByYamlKey(params, "tags." + tagName + ".type");
            switch (tagType) {
                case "plain":
                    dbType = (String) YamlUtil.getValueByYamlKey(params, "tags." + tagName + ".dbType");

                    tagValue = singleItem.getTextTrim();
                    if(YamlUtil.hasKey(params, "tags." + tagName + ".test")) {
                        String test = (String) YamlUtil.getValueByYamlKey(params, "tags." + tagName + ".test");
                        tagValue = getTestKeyValue(singleItem, test, tagValue);
                    }

                    if(tagValue.equals("") && YamlUtil.hasKey(params, "tags." + tagName + ".default")) {
                        Object defaultValue =  YamlUtil.getValueByYamlKey(params, "tags." + tagName + ".default");
                        //defaultValue = defaultValue == null ? "" : defaultValue;
                        tagValue = defaultValue.toString();
                    }

                    sqlParams.put(tagName, tagValue);
                    s = replaceFieldInSqlTemplate(s, tagName, tagValue, dbType);
                    break;
                case "foreignkey":
                    String format = (String) YamlUtil.getValueByYamlKey(params, "tags." + tagName + ".format");
                    tagValue = getForeignKeyValue(singleItem, format);
                    if(YamlUtil.hasKey(params, "tags." + tagName + ".test")) {
                        String test = (String) YamlUtil.getValueByYamlKey(params, "tags." + tagName + ".test");
                        tagValue = getTestKeyValue(singleItem, test, tagValue);
                    }
                    sqlParams.put(tagName, tagValue);
                    s = replaceFieldInSqlTemplate(s, tagName, tagValue, "varchar");
                    break;
                case "subtable":
                    subElement.add(singleItem);
                    break;
            }

        }
        if(params.get("parentParams") != null) {
            s = handleRemainField(s, (Map) params.get("parentParams"));
        }
        sqls.add(s);
        if(!subElement.isEmpty()) {
            Map m = new HashMap(params);
            Map temp = (Map) params.get("parentParams");
            if(temp != null) {
                temp.putAll(sqlParams);
                m.put("parentParams", temp);
            } else {
                m.put("parentParams", sqlParams);
            }

            for (Element e : subElement) {
                sqls.addAll(parseBoTag(e, m, parentTagName));
            }
        }

        return sqls;
    }

    private String getForeignKeyValue(Element element, String format) {
        String result = format;
        Iterator<Element> it = element.elementIterator();
        while (it.hasNext()) {
            Element singleItem = it.next();
            String tagName =singleItem.getName().toLowerCase();
            String tagValue = singleItem.getTextTrim();
            result = result.replaceAll("\\$\\{" + tagName + "\\}", Matcher.quoteReplacement(tagValue));
        }
        return result;
    }

    private String getTestKeyValue(Element element, String test, String tagValue) {
        String testResult = ExpressionUtil.parseExpression(test, tagValue);
        if(testResult.equals("")) {
            LOGGER.warn("该表达式[{}]无满足的条件，返回标签内的值", test);
            return tagValue;
        } else {
            return testResult;
        }
    }

    private String replaceFieldInSqlTemplate(String sqlTemplate, String key, Object value, String dbType) {
        String result = sqlTemplate.replaceAll("\\$\\{" + key + "\\}", Matcher.quoteReplacement(value.toString()));
        String parsedValue = "";
        switch (dbType) {
            case "varchar":
                parsedValue = "'" + value.toString() + "'";
                break;
            case "int":
            case "bit":
                parsedValue = value.toString();
                break;
        }
        return result.replaceAll("#\\{" + key + "\\}", Matcher.quoteReplacement(parsedValue));
    }

    private String handleRemainField(String sql, Map params) {
        String s = sql;
        Pattern p = Pattern.compile("[$#]\\{([a-zA-Z0-9.]+)\\}");
        Matcher matcher = p.matcher(s);
        Set<String> set = new HashSet<>();
        while (matcher.find()) {
            String key = matcher.group(1);
            if(set.contains(key)) {
                continue;
            }
            Object value = params.get(key) == null ? "?" : params.get(key);
            String dbType;
            //需优化
            if(value instanceof String) {
                dbType = "varchar";
            } else {
                dbType = "int";
            }
            s = replaceFieldInSqlTemplate(s, key, value, dbType);
            set.add(key);
        }
        return s;
    }

    @Override
    @Transactional
    public void doInsertData(InputStream in, Map config, String site) throws ImportException {
        try{
            long time1 = System.currentTimeMillis();
            List<String> sqls = parseXml(in, config);
            long time2 = System.currentTimeMillis();
            LOGGER.info("解析XML用时: {}ms", time2 - time1);
            String[] sqlArr = new String[sqls.size()];
            long time3 = System.currentTimeMillis();
            //System.out.println("Used: " + (time3 - time2));
            jdbcTemplate.batchUpdate(sqls.toArray(sqlArr));
            long time4 = System.currentTimeMillis();
            LOGGER.info("执行SQL用时: {}ms", time4 - time3);
        } catch (Exception e) {
            throw exceptionTranslate(e);
        }

    }

    private ImportException exceptionTranslate(Exception e) {
        String finalMsg;
        if(e instanceof DuplicateKeyException) {
            String nestedMsg = e.getMessage();
            Pattern p = Pattern.compile("Duplicate entry '([\\w,]+)'");
            Matcher matcher = p.matcher(nestedMsg);
            String key = null;
            if (matcher.find()) {
                key = matcher.group(1);
            }
            if (key != null && !key.equals("")) {
                finalMsg = "主键数据[" + key + "]重复";
            } else {
                finalMsg = "主键数据重复";
            }
        } else if(e instanceof DataAccessException) {
            finalMsg = "数据库操作出错";
        } else if(e instanceof DocumentException) {
            finalMsg = "XML文档解析错误";
        } else if(e instanceof ImportException) {
            //Do nothing
            return (ImportException) e;
        } else {
            finalMsg = "导入出错，请联系管理员";
        }

        ImportException ex = new ImportException(finalMsg + ": " + e.getMessage());
        ex.initCause(e);
        ex.setShortMsg(finalMsg);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ex.printStackTrace(new PrintStream(stream));
        LOGGER.error("错误信息:\n" + stream.toString());
        return ex;
    }

    @Override
    public void doTest(String site) {
        List l = jdbcTemplate.queryForList("select 1 from dual");
    }
}
