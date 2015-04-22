/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.mocks;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jephthah Sadare
 */
public class SignatureProperties {
    
    private final String HOLDER_SIGNATURE_PATH = "holder.signature.dir";
    private final String POWER_OF_ATTORNEY_PATH = "holder.powerofattorney.dir";
    
    private final Logger logger = LoggerFactory.getLogger(SignatureProperties.class);
    
    Properties prop = new Properties();
    InputStream input;
    String path = "";

    private String getPath(String pathName) {

        try {
            // input = cls.getClassLoader().getResourceAsStream("signature_powerofattorney.properties");
            input = new FileInputStream("signature_powerofattorney.properties");
            prop.load(input);
            path = prop.getProperty(pathName);
        } catch (IOException ioex) {
            logger.error("Error loading signature.properties file" + ioex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ioex) {
                }
            }
        }
        return path;
    }
    
    public String getSignaturePath() {
        return getPath(HOLDER_SIGNATURE_PATH);
    }
    
    public String getPowerOfAttorney() {
        return getPath(POWER_OF_ATTORNEY_PATH);
    }

}
