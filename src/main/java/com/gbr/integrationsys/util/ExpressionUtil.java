package com.gbr.integrationsys.util;

import org.apache.commons.jexl3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionUtil.class);

    public static String parseExpression(String test, String value) {
        String elseExpression = "";
        for (String ex : test.split(";")) {
            if(ex.startsWith("bool(")) {
                if(testBoolExpression(ex, value)) {
                    return getValue(ex, value);
                }
            } else if(ex.startsWith("else:")) {
                elseExpression = ex;
            } else if(ex.contains(":")){
                if(testPlainExpression(ex, value)) {
                    return getValue(ex, value);
                }
            }
        }

        if(!elseExpression.equals("")) {
            return getValue(elseExpression, value);
        } else {
            return "";
        }

    }

    private static boolean testPlainExpression(String test, String value) {
        String left = test.split(":")[0];
        left = left.replaceAll("(^'|'$)", "");
        return left.equals(value);
    }

    private static boolean testBoolExpression(String test, String value) {
        Pattern p = Pattern.compile("bool\\((.+)\\)\\s*:");
        Matcher m = p.matcher(test);
        String boolExpr = "";

        if(m.find()) {
            boolExpr = m.group(1);
        }
        if(!boolExpr.equals("")) {
            Object result;
            JexlEngine jexl = new JexlBuilder().create();
            JexlExpression e = jexl.createExpression(boolExpr);

            JexlContext jc = new MapContext();
            jc.set("value", value);

            try {
                result = e.evaluate(jc);
                return Boolean.parseBoolean(result.toString());
            } catch (Exception ex) {
                LOGGER.warn("布尔表达式[{}]解析错误", test);
                return false;
            }
        } else {
            LOGGER.warn("布尔表达式[{}]格式错误", test);
            return false;
        }
    }

    private static String getValue(String test, String value) {
        String s = test.substring(test.indexOf(':') + 1, test.length());
        s = s.trim().replaceAll("(^'|'$)", "");
        s = s.replaceAll("\\$\\{value\\}", value);
        return s;
    }

    public static void main(String[] args) {
        String left =  "bool(value.endsWith(',*')):'*';else:${value}";
        String s1 = parseExpression(left, "tbaseModel:A963,A963");
        String s2 = parseExpression(left, "tbaseModel:A963,*");
        System.out.println(s1 + "\n" + s2);
    }
}
