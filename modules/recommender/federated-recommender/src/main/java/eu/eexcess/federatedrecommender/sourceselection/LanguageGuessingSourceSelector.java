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

package eu.eexcess.federatedrecommender.sourceselection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import eu.eexcess.config.FederatedRecommenderConfiguration;
import eu.eexcess.dataformats.PartnerBadge;
import eu.eexcess.dataformats.userprofile.ContextKeyword;
import eu.eexcess.dataformats.userprofile.SecureUserProfile;
import eu.eexcess.federatedrecommender.interfaces.PartnerSelector;
import eu.eexcess.utils.LanguageGuesser;

public class LanguageGuessingSourceSelector implements PartnerSelector {

    private static final Logger LOGGER = Logger.getLogger(LanguageGuessingSourceSelector.class.getName());
    private Map<PartnerBadge, List<String>> selectedPartners = new HashMap<>();

    public LanguageGuessingSourceSelector(FederatedRecommenderConfiguration configuration) {
    }

    /**
     * Selects partners according to language matches. Languages may be
     * specified in the user profile. If no language is specified it will be
     * guessed from the context keywords.
     * 
     * @return the same userProfile with eventually added sources if
     *         (userProfile.partnerList.size() <= 0)
     */
    @Override
    public SecureUserProfile sourceSelect(SecureUserProfile userProfile, List<PartnerBadge> partners) {

        selectedPartners.clear();

        // don't touch if already selected
        if (userProfile.getPartnerList().isEmpty()) {
            // no query language(s) are specified; try to guess
            if (userProfile.getLanguages().isEmpty()) {
                String textFragment = joinContextKeywords(userProfile.getContextKeywords());
                String userLanguage = LanguageGuesser.getInstance().guessLanguage(textFragment);
                collectPartnersOnLanguageMatch(userLanguage, partners, userProfile.getPartnerList());
            } else {
                LOGGER.info("refusing to guess languages due to [" + userProfile.getLanguages().size() + "] already specified languages");
                return userProfile;
            }
        } else {
            LOGGER.info("refusing to select partners due to [" + userProfile.getPartnerList().size() + "] prevoiously selected partners");
            return userProfile;
        }

        if (!selectedPartners.isEmpty()) {
            LOGGER.info("context-keywords-based source selection:");
            for (Map.Entry<PartnerBadge, List<String>> entry : selectedPartners.entrySet()) {
                StringBuilder info = new StringBuilder();
                info.append("partner [" + entry.getKey().getSystemId() + "] matching language:");
                for (String language : entry.getValue()) {
                    info.append(" [" + language + "]");
                }
                LOGGER.info(info.toString());
            }
        } else {
            LOGGER.info("unsuccessfull partner selection");
        }

        return userProfile;
    }

    private String joinContextKeywords(List<ContextKeyword> contextKeywords) {
        StringBuilder builder = new StringBuilder();

        for (ContextKeyword keyword : contextKeywords) {
            builder.append(keyword.getText() + " ");
        }

        return builder.toString().trim();
    }

    /**
     * Store(s) partner(s) to userProfile if it supports the given language.
     * 
     * @param language
     *            the given language
     * @param partners
     *            list of partners to consider
     * @param partnerConnectorList
     *            where to store the partner reference if it supports the given
     *            language
     */
    private void collectPartnersOnLanguageMatch(String language, List<PartnerBadge> partners, List<PartnerBadge> partnerConnectorList) {

        if (null == language) {
            return;
        }
        for (PartnerBadge partner : partners) {
            for (String partnerLanguage : partner.getLanguageContent()) {
                if (partnerLanguage.compareTo(language) == 0 && false == partnerConnectorList.contains(partner)) {
                    partnerConnectorList.add(partner);

                    if (!selectedPartners.containsKey(partner)) {
                        List<String> newList = new ArrayList<String>();
                        newList.add(language);
                        selectedPartners.put(partner, newList);
                    } else {
                        selectedPartners.get(partner).add(language);
                    }
                }
            }
        }
    }

}
