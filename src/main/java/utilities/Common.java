package utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class Common {
    public static String VDB_NAME = System.getProperty("user.home") + "/" + ".ingestor.db";

    public static Properties SetProperties(String configFile) throws IOException {
        Properties prop = new Properties();
        InputStream in = new FileInputStream(configFile);

        prop.load(in);

        for (Enumeration e = prop.propertyNames(); e.hasMoreElements();) {
            String key = e.nextElement().toString();
            //System.out.println(key + " = " + prop.getProperty(key));
        }
        return prop;
    }
}
