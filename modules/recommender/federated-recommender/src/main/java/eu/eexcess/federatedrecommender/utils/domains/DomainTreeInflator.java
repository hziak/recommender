/**
 * Copyright (C) 2015
 * "Kompetenzzentrum fuer wissensbasierte Anwendungen Forschungs- und EntwicklungsgmbH" 
 * (Know-Center), Graz, Austria, office@know-center.at.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Raoul Rubien
 */

package eu.eexcess.federatedrecommender.utils.domains;

import java.io.IOException;

import eu.eexcess.federatedrecommender.utils.tree.TreeNode;

/**
 * An domain tree inflator interface.
 * @author Raoul Rubien
 *
 */
public interface DomainTreeInflator {

    /**
     * interface method to return a complete domain tree
     * 
     * @param wordnetCSVTreeFile
     * @return
     * @throws IOException
     */
    public TreeNode inflateDomainTree() throws IOException;

}
