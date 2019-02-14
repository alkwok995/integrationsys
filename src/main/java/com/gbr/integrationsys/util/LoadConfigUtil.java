package com.gbr.integrationsys.util;

import com.gbr.integrationsys.exception.ImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Component
public class LoadConfigUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadConfigUtil.class);

    public Map<String, Object> loadConfigInFolder(String configName) throws FileNotFoundException {
        LOGGER.info("Refresh import configuration with name [{}] in folder mode.", configName);

        URL url = LoadConfigUtil.class.getClassLoader().getResource("importConfig/" + configName + ".yml");
        if(url == null) {
            throw new FileNotFoundException("配置文件[importConfig/" + configName + ".yml]未能找到");
        }
        String mainPath = url.getPath();
        InputStream is= new FileInputStream(mainPath);

        Yaml yaml = new Yaml();
        return yaml.load(is);
    }

    public Map<String, Object> loadAllConfigInFolder() throws FileNotFoundException {
        LOGGER.info("Refresh all import configurations in folder mode.");

        Map<String, Object> result = new HashMap<>();
        Yaml yaml = new Yaml();
        String path = LoadConfigUtil.class.getResource("/").getPath();
        File resourceFile = new File(path + "/importConfig");

        File[] files = resourceFile.listFiles();
        if (files != null) {
            for (File f : files) {
                if(f.isFile()) {
                    String fileName = f.getName().substring(0, f.getName().indexOf("."));
                    result.put(fileName, yaml.load(new FileInputStream(f)));
                }
            }
        } else {
            LOGGER.warn("importConfig中未发现配置文件");
        }

        return result;
    }

    public Map<String, Object> loadConfigInJar(String configName) {
        LOGGER.debug("Refresh import configuration with name [{}] in jar mode.", configName);

        InputStream is = LoadConfigUtil.class.getClassLoader()
                .getResourceAsStream("/importConfig/" + configName + ".yml");
        Yaml yaml = new Yaml();
        return yaml.load(is);
    }

    public Map<String, Object> loadAllConfigInJar() throws IOException {
        LOGGER.debug("Refresh all import configurations in jar mode.");

        Map<String, Object> result = new HashMap<>();
        Yaml yaml = new Yaml();

        String path = LoadConfigUtil.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        path = path.substring(path.indexOf('/') + 1, path.indexOf('!'));
        JarFile jarFile = new JarFile(path);

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String innerPath = entry.getName();
            if(innerPath.startsWith("BOOT-INF/classes/importConfig") && innerPath.endsWith(".yml")) {
                InputStream is = LoadConfigUtil.class.getClassLoader()
                        .getResourceAsStream(innerPath);
                String pathName = innerPath.substring(innerPath.lastIndexOf('/'), innerPath.indexOf('.'));
                result.put(pathName, yaml.load(is));
            }
        }

        return result;
    }

    public Map<String, Object> loadSingleConfig(String configName) throws ImportException {
        Map<String, Object> result;
        try {
            return isConfigInJar() ? loadConfigInJar(configName) : loadConfigInFolder(configName);
        } catch (Exception e) {
            throw exceptionTranslate(e);
        }
    }

    public Map loadAllConfig() throws ImportException {
        Map result;
        try {
            return isConfigInJar() ? loadAllConfigInJar() : loadAllConfigInFolder();
        } catch (Exception e) {
            throw exceptionTranslate(e);
        }

    }

    private ImportException exceptionTranslate(Exception e) {
        String finalMsg;
        if(e instanceof FileNotFoundException) {
            finalMsg = "配置文件未能找到";
        } else if(e instanceof IOException) {
            finalMsg = "Jar包内配置文件解析错误";
        } else {
            finalMsg = "加载配置文件错误";
        }

        ImportException ex = new ImportException(finalMsg + ": " + e.getMessage());
        ex.initCause(e);
        ex.setShortMsg(finalMsg);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ex.printStackTrace(new PrintStream(stream));
        LOGGER.error("错误信息:\n" + stream.toString());
        return ex;
    }

    private boolean isConfigInJar() {
        String path = LoadConfigUtil.class.getResource("/").getPath();
        return path.contains("!");
    }

    /*
    public static void main(String[] args) {
        try {
            throw exceptionTranslator(new IOException("nOT FOUND"), "BbBA");
        } catch (ImportException e) {
            //e.printStackTrace();
        }
    }*/
}
