/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.impl.associations.filter;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.associations.AssociationMatchFilter;
import org.rioproject.entry.VersionEntry;
import org.rioproject.version.VersionMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@code AssociationMatchFilter} that matches on service version(s).
 *
 * </br>
 * Version matching is done using service published {@link org.rioproject.entry.VersionEntry}
 * attributes compared to declared version requirements as follows: &nbsp; <br>
 * <br>
 * <table cellpadding="2" cellspacing="2" border="1"
 * style="text-align: left; width: 100%;">
 * <tbody>
 * <tr>
 * <th style="vertical-align: top;">Requirement<br>
 * </th>
 * <th style="vertical-align: top;">Support Criteria<br>
 * </th>
 * </tr>
 * <tr>
 * <td style="vertical-align: top;">1.2.7<br>
 * </td>
 * <td style="vertical-align: top;">Specifies an exact version<br>
 * </td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top;">2*<br>
 * </td>
 * <td style="vertical-align: top;">Supported for all minor versions of 2 <br>
 * </td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top;">3.4*<br>
 * </td>
 * <td style="vertical-align: top;">Supported for all minor versions of 3.4, including 3.4<br>
 * </td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top;">1.2+<br>
 * </td>
 * <td style="vertical-align: top;">Supported for version 1.2 or
 * above&nbsp; </td>
 * </tr>
 * </tbody>
 * </table>
 * </br>
 *
 * @see AssociationDescriptor
 *
 * @author Dennis Reedy
 */
public class VersionMatchFilter implements AssociationMatchFilter {
    private final VersionMatcher versionMatcher = new VersionMatcher();
    private final Logger logger = LoggerFactory.getLogger(VersionMatchFilter.class);

    public boolean check(AssociationDescriptor descriptor, ServiceItem serviceItem) {
        if(descriptor.getVersion()==null)
            return true;
        boolean matches = false;
        String configuredVersion = descriptor.getVersion();
        for(Entry entry : serviceItem.attributeSets) {
            if(entry instanceof VersionEntry) {
                matches = versionMatcher.versionSupported(configuredVersion, ((VersionEntry) entry).version);
                if(logger.isDebugEnabled()) {
                    logger.debug("requiredVersion: {}, publishedVersion: {}, matched? {}",
                                 configuredVersion, entry, matches);
                }
                if(matches)
                    break;
            }
        }
        return matches;
    }


}
