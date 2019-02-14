package com.gbr.integrationsys.util;

import org.dom4j.Element;

public class ElementUtil {
    public static Element getFirstChildElement(Element e) {
        Element result = e.elements().size() == 0 ? null : (Element) e.elements().get(0);
        return result;
    }
}
