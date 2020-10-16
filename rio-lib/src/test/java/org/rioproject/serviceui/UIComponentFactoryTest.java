package org.rioproject.serviceui;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;

public class UIComponentFactoryTest {

    @Test
    public void testStringExpand() throws MalformedURLException {
        UIComponentFactory uiComponentFactory = new UIComponentFactory("file://${user.name}/foo/${user.home}",
                                                                       UIComponentFactoryTest.class.getName());
        URL[] urls = uiComponentFactory.expandUrlString();
        assertEquals(1, urls.length );
    }

}