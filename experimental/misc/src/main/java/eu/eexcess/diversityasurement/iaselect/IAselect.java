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

package eu.eexcess.diversityasurement.iaselect;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * Implementation of IA-Select described in [Agrawal, R., Gollapudi, S.,
 * Halverson, A., & Ieong, S. (2009). Diversifying search results. In
 * Proceedings of the Second ACM International Conference on Web Search and Data
 * Mining - WSDM ’09 (p. 5). New York, New York, USA: ACM Press.
 * http://doi.org/10.1145/1498759.1498766].
 * 
 * @author Raoul Rubien
 */
public class IAselect {

	private Document maxMarginalUtilityDocument = null;
	private double maxMarginalUtility = -1;

	/**
	 * IASelect(k, q, C(q), R(q), C̶(̶d̶)̶,̶ P̶(̶c̶|̶q̶)̶, V (d|q, c)) - greedy
	 * algorithm to diversify(k) and maximize P(S|q)
	 * 
	 * @param k
	 *            top k documents to reorder
	 * @param q
	 *            query for ranking documents
	 * @param Cq
	 *            C(q) - set of categories query belongs to
	 * @param Rq
	 *            R(q) - top k ranked documents
	 * @̶p̶a̶r̶a̶m̶ ̶D̶q̶ ̶C̶(̶d̶)̶ ̶-̶ ̶s̶e̶t̶ ̶o̶f̶ ̶c̶a̶t̶e̶g̶o̶r̶i̶e̶s̶
	 *              ̶d̶o̶c̶u̶m̶e̶n̶t̶s̶ ̶b̶e̶l̶o̶n̶g̶s̶ ̶t̶o̶
	 * @̶p̶a̶r̶a̶m̶ ̶P̶c̶q̶ ̶P̶(̶c̶|̶q̶)̶ ̶-̶ ̶d̶i̶s̶t̶r̶i̶b̶u̶t̶i̶o̶n̶ ̶o̶f̶
	 *              ̶p̶r̶o̶b̶a̶b̶i̶l̶i̶t̶y̶ ̶t̶h̶a̶t̶ ̶c̶a̶t̶e̶g̶o̶r̶y̶ ̶c̶
	 *              ̶b̶e̶l̶o̶n̶g̶s̶ ̶t̶o̶ ̶q̶u̶e̶r̶y̶ ̶q̶
	 * @param Vdqc
	 *            V(d|q,c) - document quality for query q qhen intended category
	 *            is c
	 * @return resorted list of k documents out of R(q)
	 */
	Set<Document> IASelect(int k, Query q, Set<Category> Cq, Set<Document> Rq, DocumentQualityValueV V) {
		Set<Document> R = Rq;
		Set<Document> S = new LinkedHashSet<Document>(k);

		ConditionalProbabilityU U = new ConditionalProbabilityU(Cq);
		System.out.println(U);

		while (S.size() < k) {
			System.out.println("\n[" + S.size() + "] out of [" + k + "] needed documents selected");
			System.out.println(sToString(S));

			clearMaxMarginalUtility();
			for (Document d : R) {
				g(d, q, S, U, V);
			}
			Document dMax = argmax();
			S.add(dMax);

			System.out.println("select maxarg(g(d|q,c,S=" + sToString(S) + ")=" + maxMarginalUtility + ")=" + dMax.name);
			/**
			 * for all c ∈ C(d*) AND "c ∈ C(q)" because but P(c|q) always refers
			 * to c ∈ C(q), see 3.1
			 */
			for (Category c : C(dMax)) {
				if (Cq.contains(c)) {
					U.updateU(q.getCategory(c), q, S, dMax, V);
					System.out.println(U);
				}
			}
			R.remove(dMax);
		}
		return S;
	}

	private String sToString(Set<Document> S) {
		return new StringBuilder().append("S:{").append(StringUtils.join(S.toArray(new Document[0]), ", ")).append("}")
						.toString();
	}

	private void clearMaxMarginalUtility() {
		maxMarginalUtility = -1;
	}

	/**
	 * fetches the document with the highest marginal utility g(d|q,c,S)
	 * 
	 * @return
	 */
	Document argmax() throws IllegalStateException {
		if (maxMarginalUtility < 0) {
			throw new IllegalStateException("no argmax calculated at this time");
		}
		return maxMarginalUtilityDocument;
	}

	/**
	 * greedy algorithm to diversify(k) and maximize P(S|q)
	 * 
	 * @param k
	 *            number of documents to re-rank out of classical ranked
	 *            document set
	 * @param q
	 *            query to re-rank documents
	 * @param Rq
	 *            R(q) - top documents returned by a classical ranking algorithm
	 *            for query q
	 * @param V
	 *            document qualities for ∀ d ∈ R(q)
	 * @return re-ranked document list with |R(q)| = k
	 */
	public Set<Document> IASelect(int k, Query q, Set<Document> Rq, DocumentQualityValueV V) {
		return IASelect(k, q, C(q), Rq, V);
	}

	/**
	 * C(m) - fetches the set of categories to which a query|document m belongs to
	 * 
	 * @param m
	 *            also known as q (query) or d (document)
	 * @return set of categories m belongs to
	 */
	Set<Category> C(MessageCategories m) {
		return m.categories();
	}

	/**
	 * g(d|q,c,S) - calculated the highest marginal utility as a product of:
	 * <p>
	 * U(c,q,S) * V(d,q,c)
	 * 
	 * @param d
	 *            document
	 * @param q
	 *            query
	 * @param S
	 *            already selected documents
	 */
	void g(Document d, Query q, Set<Document> S, ConditionalProbabilityU U, DocumentQualityValueV V) {
		double sum = 0;

		/**
		 * for all c ∈ C(d) AND "c ∈ C(q)" because but P(c|q) always refers to c
		 * ∈ C(q), see 3.1
		 */
		for (Category c : C(d)) {
			if (C(q).contains(c)) {
				sum += U.U(c, q, S) * V.V(d, q, c);
			}
		}
		if (maxMarginalUtility < sum) {
			maxMarginalUtility = sum;
			maxMarginalUtilityDocument = d;
			System.out.println("argmx=" + maxMarginalUtility + " d=" + d.name);
		}
	}
}