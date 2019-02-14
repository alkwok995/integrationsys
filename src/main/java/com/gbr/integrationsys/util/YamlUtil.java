package com.gbr.integrationsys.util;


import com.gbr.integrationsys.exception.ImportException;

import java.util.Map;

public class YamlUtil {
    public static Object getValueByYamlKey(Map m, String keys) throws ImportException {
        String[] keyArray = keys.split("\\.");
        Map tempMap = m;
        for (int i = 0; i < keyArray.length - 1; i++) {
            Object item = tempMap.get(keyArray[i]);
            if(item instanceof Map) {
                tempMap = (Map) tempMap.get(keyArray[i]);
            } else {
                throw new ImportException("YML配置文件中不存在键[" + keys +"]").setShortMessage("YML配置文件出错");
            }
        }

        Object result = tempMap.get(keyArray[keyArray.length - 1]);
        if(result == null) {
            throw new ImportException("YML配置文件中不存在键[" + keys +"]").setShortMessage("YML配置文件出错");
        }
        return result;
    }

    public static boolean hasKey(Map m, String keys) {
        try {
            getValueByYamlKey(m, keys);
        } catch (ImportException e) {
            return false;
        }
        return true;
    }

/*
    public static void main(String[] args) {
        *//*try {
            Map m = parseYamlByPath("importConfig/materialData.yml");
            String s = "tags.site1.type1";
            String k = (String) getValueByYamlKey(m, s);
            System.out.println(k);
        } catch (Exception e) {
            e.printStackTrace();
        }*//*


        *//*format = format.replaceAll("\\$\\{" + "site" + "\\}", "a888");
        format = format.replaceAll("\\$\\{" + "routecode" + "\\}", "baba");*//*
        //System.out.println(format);
    }*/

}
