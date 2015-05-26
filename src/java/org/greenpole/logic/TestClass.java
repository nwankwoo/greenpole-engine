/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Akinwale Agbaje
 */
public class TestClass {
    private static final Logger logger = LoggerFactory.getLogger(ClientCompanyComponentLogic.class);
    
    public void runthis() {
        logger.info("Testing here");
    }
    public static void main(String[] args) {
        new TestClass().runthis();
    }
}
