/* Copyright (C) 2014
"JOANNEUM RESEARCH Forschungsgesellschaft mbH" 
 Graz, Austria, digital-iis@joanneum.at.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package eu.eexcess.zbw.datalayer;

import org.w3c.dom.Document;

import com.hp.hpl.jena.query.QuerySolution;

import eu.eexcess.dataformats.result.Result;
import eu.eexcess.dataformats.result.ResultList;
import eu.eexcess.partnerdata.reference.Transformer;

public class ZBWTransformer extends Transformer{
	
	
	@Override
	protected Result postProcessResult(Document orgPartnerResult, Result result, QuerySolution querySol) {
		result.documentBadge.uri = "http://www.econbiz.de/Record/"+ result.documentBadge.id;
		if (result.mediaType == null || result.mediaType.trim().isEmpty() || result.mediaType.equalsIgnoreCase(EEXCESS_FACETS_VALUE_UNKNOWN))
			result.mediaType = EEXCESS_MEDIATYPE_TEXT;
		return result;
	}

	@Override
	protected ResultList postProcessResults(Document orgPartnerResult, ResultList resultList) {
		resultList.totalResults = Integer.parseInt(getValueWithXPath("/doc/hits/total", orgPartnerResult));
		return resultList;
	}

}
