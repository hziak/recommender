/* Copyright (C) 2014 
"Kompetenzzentrum fuer wissensbasierte Anwendungen Forschungs- und EntwicklungsgmbH" 
(Know-Center), Graz, Austria, office@know-center.at.

Licensees holding valid Know-Center Commercial licenses may use this file in
accordance with the Know-Center Commercial License Agreement provided with 
the Software or, alternatively, in accordance with the terms contained in
a written agreement between Licensees and Know-Center.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.eexcess.federatedrecommender.utils.esuputils;

import java.io.Serializable;

/**
 * ExtendedSecureUserProfileLink support class
 * 
 * @author hziak
 *
 */
public class ESUPLink implements Serializable {
    private static final long serialVersionUID = -311440128549127093L;
    private String className;
    private Double link;

    public ESUPLink(String className, Double link) {
        this.setClassName(className);
        this.setLink(link);
    }

    public ESUPLink(ESUPLink esupLink) {
        this.className = new String(esupLink.className);
        this.link = new Double(esupLink.getLink());
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Double getLink() {
        return link;
    }

    public void setLink(Double link) {
        this.link = link;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + ((link == null) ? 0 : link.hashCode());
        return result;
    }

    /**
     * Careful, Equals overwritten
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof ESUPLink) {
            if (((ESUPLink) o).getClassName().equals(this.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ESUPLink [className=" + className + ", link=" + link + "]";
    }

}