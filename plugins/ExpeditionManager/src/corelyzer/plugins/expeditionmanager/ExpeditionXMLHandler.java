package corelyzer.plugins.expeditionmanager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import corelyzer.plugins.expeditionmanager.data.DataStoreFactory;
import corelyzer.plugins.expeditionmanager.data.IDataStore;
import corelyzer.plugins.expeditionmanager.handlers.IDataHandler;

/**
 * Populates an Expedition from an XML description.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ExpeditionXMLHandler extends DefaultHandler {
    private final Expedition expedition;
    private IDataStore dataStore;
    private IDataHandler dataHandler;
    private Properties properties;
    private StringBuffer buffer;
    private String key;

    /**
     * Creates a new ExpeditionXMLHandler.
     * 
     * @param expedition
     *            the expedition.
     */
    public ExpeditionXMLHandler(final Expedition expedition) {
        this.expedition = expedition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        final StringBuffer temp = new StringBuffer();
        for (int i = start; i < start + length; i++) {
            temp.append(ch[i]);
        }
        buffer.append(temp.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String name) throws SAXException {
        if (name.equals("DataStore")) {
            if ((dataStore != null) && (dataHandler != null)) {
                dataHandler.setDataStore(dataStore);
                expedition.addDataStore(dataStore, dataHandler);
            }
        } else if (name.equals("DataHandler")) {
            if ((dataHandler != null) && (properties != null)) {
                dataHandler.setProperties(properties);
            }
        } else if (name.equals("entry")) {
            if ((key != null) && (properties != null)) {
                properties.put(key, buffer.toString());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String name, final Attributes attributes) throws SAXException {
        // create a buffer to catch our characters
        buffer = new StringBuffer();

        // handle specific tags
        if (name.equals("Expedition")) {
            expedition.setName(attributes.getValue("name"));

            // set the logo URL if available
            String logo = attributes.getValue("logo");
            if (logo != null) {
                try {
                    expedition.setLogo(new URL(logo));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }

            // set the root URL if available
            String root = attributes.getValue("root");
            if (root != null) {
                try {
                    expedition.setRoot(new URL(root));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        } else if (name.equals("DataStore")) {
            // reset our objects
            dataStore = null;
            dataHandler = null;

            // pull out the key attributes
            String id = attributes.getValue("name");
            String category = attributes.getValue("category");
            String type = attributes.getValue("type");
            String path = attributes.getValue("path");
            if ((path != null) && (type != null)) {
                dataStore = DataStoreFactory.create(expedition, path, id,
                        category, type);
            }
        } else if (name.equals("DataHandler")) {
            // create a new set of properties
            properties = new Properties();
            key = null;

            // try instantiating our handler
            String handlerClass = attributes.getValue("class");
            if (handlerClass != null) {
                try {
                    Class<?> clazz = Class.forName(handlerClass);
                    if ((clazz != null)
                            && IDataHandler.class.isAssignableFrom(clazz)) {
                        dataHandler = (IDataHandler) clazz.newInstance();
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } else if (name.equals("entry")) {
            key = attributes.getValue("key");
        }
    }
}
