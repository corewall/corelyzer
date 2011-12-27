package corelyzer.plugin.iCores.helper;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CoreSectionInfoRetrival {
    Element xmlRoot;

    public static String getValueByKey(Element e, String key) {
        if(e != null) {
            NodeList list = e.getElementsByTagName(key);
            return list.item(0).getTextContent();
        }

        return "Undefined " + key;
    }

    public CoreSectionInfoRetrival() {
        super();
    }

    public CoreSectionInfoRetrival(Element e) {
        this();
        xmlRoot = e;
    }

    // Available keys:
    // name, range, URL, dpi_x, dpi_y, size, format...
    public String getValueByKey(String key) {
        if(xmlRoot != null) {
            NodeList list = xmlRoot.getElementsByTagName(key);
            return list.item(0).getTextContent();
        }

        return "Undefined " + key;        
    }
}
