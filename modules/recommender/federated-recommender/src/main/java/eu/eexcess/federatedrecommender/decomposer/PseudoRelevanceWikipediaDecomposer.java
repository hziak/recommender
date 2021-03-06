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
package eu.eexcess.federatedrecommender.decomposer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.LocaleUtils;

import at.knowcenter.commons.wikipedia.queryexpansion.WikipediaQueryExpansion;
import at.knowcenter.util.term.TermSet;
import at.knowcenter.util.term.TypedTerm;
import eu.eexcess.config.FederatedRecommenderConfiguration;
import eu.eexcess.dataformats.userprofile.ContextKeyword;
import eu.eexcess.dataformats.userprofile.ExpansionType;
import eu.eexcess.dataformats.userprofile.Language;
import eu.eexcess.dataformats.userprofile.SecureUserProfile;
import eu.eexcess.federatedrecommender.interfaces.SecureUserProfileDecomposer;
import eu.eexcess.federatedrecommender.utils.FederatedRecommenderException;
import eu.eexcess.utils.LanguageGuesser;
/**
 * Class to provide query expansion from Wikipedia
 * 
 * @author hziak
 *
 */
public class PseudoRelevanceWikipediaDecomposer implements SecureUserProfileDecomposer<SecureUserProfile,SecureUserProfile> {

	private static final Logger logger = Logger.getLogger(PseudoRelevanceWikipediaDecomposer.class.getName());
	
	private Map<String, WikipediaQueryExpansion> localeToQueryExpansion;
	 private static final String[] SUPPORTED_LOCALES = new String[] { "en", "de" };
	

	/**
	 * number of terms to be expanded in {@link #decompose(SecureUserProfile)}
	 */
	private int numTermsToExpand = 10;

	/**
	 * 
	 * @param wikipediaBaseIndexDir the base directory for the Wikipedia indices, it is expected to contain folders like "enwiki" and "dewiki"
	 * @throws IOException
	 */
	
	public PseudoRelevanceWikipediaDecomposer() throws IOException {
		
		
	}

	@Override
	public SecureUserProfile decompose(SecureUserProfile inputSecureUserProfile) {
		
		TermSet<TypedTerm> terms = new TermSet<TypedTerm>(new TypedTerm.AddingWeightTermMerger());
		StringBuilder builder = new StringBuilder();
	
		for (ContextKeyword keyword : inputSecureUserProfile.getContextKeywords()) {
			if (builder.length() > 0) { builder.append(" "); }
			builder.append(keyword.getText());
			terms.add(new TypedTerm(keyword.getText(), null, 1));
		}
		String query = builder.toString();
		
		String localeName = null;
		// first, pick up the language specified by the user
		if (inputSecureUserProfile.getLanguages() != null && !inputSecureUserProfile.getLanguages().isEmpty()) {
			Language firstLanguage = inputSecureUserProfile.getLanguages().iterator().next();
			localeName = firstLanguage.getIso2();
		} else {
			// then try to detect the language from the query
			String guessedLanguage = LanguageGuesser.getInstance().guessLanguage(query);
			if (guessedLanguage != null) {
				localeName = guessedLanguage;
			}
		}
		
		WikipediaQueryExpansion wikipediaQueryExpansion = localeToQueryExpansion.get(localeName);
		if (wikipediaQueryExpansion == null) {
			// no query expansion for the current locale, fall back to the first supported locale
			wikipediaQueryExpansion = localeToQueryExpansion.get(SUPPORTED_LOCALES[0]);
		}

		try {
			TermSet<TypedTerm> queryExpansionTerms;
			queryExpansionTerms = wikipediaQueryExpansion.expandQuery(query);
			terms.addAll(queryExpansionTerms.getTopTerms(numTermsToExpand));
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Cannot expand the query using Wikipedia", e);
		}
		
		List<ContextKeyword> newContextKeywords = new ArrayList<ContextKeyword>();
		for (TypedTerm typedTerm : terms.getTopTerms(numTermsToExpand)) {
			newContextKeywords.add(new ContextKeyword(typedTerm.getText(),ExpansionType.PSEUDORELEVANCEWP));
		}
		inputSecureUserProfile.getContextKeywords().addAll(newContextKeywords);
		logger.log(Level.INFO, "Wikipedia Expansion: " + newContextKeywords.toString());
		return inputSecureUserProfile;
	}
	
	/**
	 * Set number of terms to be expanded on future calls to {@link #decompose(SecureUserProfile)} default: {@link #numTermsToExpand}
	 * @param numTerms
	 */
	public void setMaxNumTermsToExpand(int numTerms) {
		numTermsToExpand = numTerms;
	}

	@Override
	public void setConfiguration(FederatedRecommenderConfiguration fedRecConfig) throws FederatedRecommenderException {
		localeToQueryExpansion = new HashMap<String, WikipediaQueryExpansion>();
		for (String localeName : SUPPORTED_LOCALES) {
			Locale locale = LocaleUtils.toLocale(localeName);
			try {
				localeToQueryExpansion.put(localeName, new WikipediaQueryExpansion(new File(fedRecConfig.getWikipediaIndexDir(), locale+"wiki"), locale));
			} catch (IOException e) {
				throw new FederatedRecommenderException("Could not intialize WikipediaQueryExpansion",e);
			}
			
		}
		
	}
}
