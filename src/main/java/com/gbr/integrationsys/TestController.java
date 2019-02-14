package com.gbr.integrationsys;

import com.gbr.integrationsys.datasource.DynamicDataSourceHolder;
import com.gbr.integrationsys.service.XmlParseService;
import com.gbr.integrationsys.util.LoadConfigUtil;
import com.gbr.integrationsys.util.PrivateMsg;
import com.gbr.integrationsys.util.YamlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestController.class);
    private Map<String, Map> yamls = new HashMap<>();

    @Autowired
    private XmlParseService parseService;

    @Autowired
    private LoadConfigUtil configLoader;

    @RequestMapping("/")
    public PrivateMsg testConn(String site) {
        String msg = "success='yes'";
        try {
            DynamicDataSourceHolder.putDataSource(site);
            parseService.doTest(site);
        } catch (Exception e) {
            msg = "fail";
            e.printStackTrace();
        }
        PrivateMsg msgObj = new PrivateMsg("Customer", msg);
        return msgObj;
    }

    @RequestMapping(value="/fuckworld/{configName}/{key}", method = RequestMethod.GET)
    public PrivateMsg fuckWorld(@PathVariable("configName")String configName, @PathVariable("key")String key) {
        Map m = yamls.get(configName);
        String msg;
        try {
            msg = (String) YamlUtil.getValueByYamlKey(m, key);
        } catch (Exception e) {
            e.printStackTrace();
            msg = e.getMessage();
        }
        PrivateMsg msgObj = new PrivateMsg("Customer", msg);
        return msgObj;
    }

    @RequestMapping(value="/refreshConfig", method = RequestMethod.GET)
    public PrivateMsg refreshAllConfig() throws Exception{
        String msg;
        Yaml yaml = new Yaml();

        msg = "success";
        yamls.putAll(configLoader.loadAllConfig());

        PrivateMsg msgObj = new PrivateMsg("Info", msg);
        return msgObj;
    }

    @RequestMapping(value="/refreshConfig/{configName}", method = RequestMethod.GET)
    public PrivateMsg refreshConfig(@PathVariable("configName") String configName) throws Exception{
        String msg;

        yamls.put(configName, configLoader.loadSingleConfig(configName));
        msg = "success";

        PrivateMsg msgObj = new PrivateMsg("Info", msg);
        return msgObj;
    }

    @RequestMapping("/importData")
    public PrivateMsg importData(@RequestParam("xmlFile")MultipartFile xmlFile, @RequestParam("configName")String configName,
                                 @RequestParam("user")String user, @RequestParam("site")String site) throws Exception{
        String msg ="success='yes'";
        LOGGER.info("=================================================================");
        LOGGER.info("来自{}的用户{}使用{}配置进行数据导入", site, user, configName);

        Map m = yamls.get(configName);
        if(m == null) {
            m = configLoader.loadSingleConfig(configName);
            yamls.put(configName, m);
        }
        Map n = new HashMap();
        n.put("user", user);
        m.put("parentParams", n);
        DynamicDataSourceHolder.putDataSource(site);
        parseService.doInsertData(xmlFile.getInputStream(), m, site);

        LOGGER.info("导入成功");
        return new PrivateMsg("customer", msg);
    }
}
