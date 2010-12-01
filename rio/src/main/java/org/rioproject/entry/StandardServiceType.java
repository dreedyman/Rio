/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.entry;

import net.jini.lookup.entry.ServiceType;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Implementation of a ServiceType
 *
 * @author Dennis Reedy
 */
public class StandardServiceType extends ServiceType {
    static final long serialVersionUID = 1L;
    public String   name="";
    public String   description="";
    public  URL     iconURL=null;

    public StandardServiceType(){
    }

    public StandardServiceType(String name){
        this.name=name;
    }

    public String getDisplayName(){
        return(name);
    }

    public void setIconURL(URL iconURL){
        this.iconURL=iconURL;
    }

    public Image getIcon(int iconKind){
        /* iconKind should conform to either:
         * 		BeanInfo.ICON_MONO_16x16
         * 		BeanInfo.ICON_MONO_32x32
         * 		BeanInfo.ICON_COLOR_16x16
         * 		BeanInfo.ICON_COLOR_32x32
         * We will return the same regardless of whats passed in 
         */
        Image iconImage=null;
        if(iconURL!=null) {
            ImageIcon icon = new ImageIcon(iconURL);
            iconImage = icon.getImage();
        }
        return(iconImage);
    }

    public String getShortDescription(){
        return(description);
    }
}

