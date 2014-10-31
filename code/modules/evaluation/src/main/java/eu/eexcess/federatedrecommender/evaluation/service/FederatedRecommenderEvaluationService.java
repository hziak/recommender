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
package eu.eexcess.federatedrecommender.evaluation.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.sun.jersey.spi.resource.Singleton;

import eu.eexcess.config.FederatedRecommenderConfiguration;
import eu.eexcess.dataformats.PartnerBadge;
import eu.eexcess.dataformats.evaluation.EvaluationResponse;
import eu.eexcess.dataformats.evaluation.EvaluationResultLists;
import eu.eexcess.dataformats.result.ResultList;
import eu.eexcess.dataformats.userprofile.ContextKeyword;
import eu.eexcess.dataformats.userprofile.SecureUserProfileEvaluation;
import eu.eexcess.dataformats.userprofile.UserCredentials;
import eu.eexcess.federatedrecommender.evaluation.FederatedRecommenderEvaluationCore;
import eu.eexcess.federatedrecommender.utils.FederatedRecommenderException;
import eu.eexcess.federatedrecommenderservice.FederatedRecommenderService;
@Path("/recommender")
@Singleton
public class FederatedRecommenderEvaluationService extends FederatedRecommenderService{
	private static final Logger logger = Logger
			.getLogger(FederatedRecommenderService.class.getName());
	private FederatedRecommenderEvaluationCore fREC;
	private final FederatedRecommenderConfiguration federatedRecommenderConfiguration;

	
	public FederatedRecommenderEvaluationService() throws FederatedRecommenderException{
		super();
		ObjectMapper mapper = new ObjectMapper();
		URL resource = getClass().getResource(
				"/federatedRecommenderConfig.json");
		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		try {
			federatedRecommenderConfiguration = mapper.readValue(new File(
					resource.getFile()),
					FederatedRecommenderConfiguration.class);
		} catch (JsonParseException e) {
			logger.log(
					Level.SEVERE,
					"There was an error parsing the FederationRecommenderConfig File",
					e);
			throw new FederatedRecommenderException(
					"There was an error parsing the FederationRecommenderConfig File in FederatedRecommenderCore Module",
					e);
		} catch (JsonMappingException e) {
			logger.log(
					Level.SEVERE,
					"There was an error parsing the FederationRecommenderConfig File",
					e);
			throw new FederatedRecommenderException(
					"There was an error parsing the FederationRecommenderConfig File in FederatedRecommenderCore Module",
					e);
		} catch (IOException e) {
			logger.log(
					Level.SEVERE,
					"There was an error reading the FederationRecommenderConfig File",
					e);
			throw new FederatedRecommenderException(
					"There was an error reading the FederationRecommenderConfig File in FederatedRecommenderCore Module",
					e);
		}
		try {
			fREC = new FederatedRecommenderEvaluationCore(federatedRecommenderConfiguration);
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, "FederatedRecommenderEvaluationCore not working", e);
		}
	}
	
	// Begin Evaluation Services
	@POST
	@Path("/testSUPE")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public SecureUserProfileEvaluation testSUPE() {
		SecureUserProfileEvaluation secureUserProfile = new SecureUserProfileEvaluation();

		secureUserProfile.firstName = "Max";
		secureUserProfile.lastName = "Musterman";
		secureUserProfile.birthDate = new Date();
		secureUserProfile.gender = "male";

		List<ContextKeyword> contextList = new ArrayList<ContextKeyword>();
		contextList.add(new ContextKeyword("graz", 0.5));
		contextList.add(new ContextKeyword("vienna", 0.5));
		secureUserProfile.contextKeywords = contextList;
		PartnerBadge pB = new PartnerBadge();
		pB.setSystemId("Europeana");
		secureUserProfile.partnerList.add(pB);

		List<PartnerBadge> protectedPartnerList = new ArrayList<PartnerBadge>();
		PartnerBadge badge = new PartnerBadge();
		badge.setSystemId("Wissenmedia");
		badge.partnerKey = "dsajln22sadjkl!";
		protectedPartnerList.add(badge);

		secureUserProfile.protectedPartnerList = protectedPartnerList;

		List<UserCredentials> UserCredentials = new ArrayList<UserCredentials>();
		eu.eexcess.dataformats.userprofile.UserCredentials cred = new UserCredentials();
		cred.login = "me@partner.x";
		cred.securityToken = "sdjalkej21!#";
		cred.systemId = "Wissenmedia";
		UserCredentials.add(cred);
		secureUserProfile.userCredentials = UserCredentials;
		secureUserProfile.queryExpansionSourcePartner = (ArrayList<PartnerBadge>) secureUserProfile.partnerList;
		return secureUserProfile;
	}

	@POST
	@Path("/evaluation")
	@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	@Deprecated
	public ResultList evaluation(SecureUserProfileEvaluation userProfile)
			throws IOException {
		ResultList resultList = fREC.getEvaluationResults(userProfile);
		return resultList;
	}

	/**
	 * takes the already evaluated result and returns if there is a query left
	 * to evaluate
	 * 
	 * @param id
	 * @param result
	 * @return
	 * @throws IOException
	 */
	@POST
	@Path("/evaluationResultUID")
	@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response evaluationResultUID(@QueryParam("id") Integer id,
			EvaluationResultLists result) throws IOException {
		EvaluationResponse eR = new EvaluationResponse();

		eR.evaluationDone = fREC.logEvaluationResult(id, result);

		logger.log(Level.INFO, "Result from user " + id + " retrieved ");
		return Response.ok(eR).build();
	};

	/**
	 * 
	 * returns the results to evaluate
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 */
	@POST
	@Path("/evaluationWithUID")
	// @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response evaluationWithUID(@QueryParam("id") Integer id)
			throws IOException {
		
		EvaluationResultLists results = fREC.getExpansionEvaluation(id);

		return Response.ok(results).build();
		// return Response.ok().build();

	}

	/**
	 * writes out all result to result files and removes the result from the
	 * query manager
	 * 
	 * @return
	 */
	@GET
	@Path("/evaluationWriteOutAllResults")
	public Response evaluationWriteOutAllResults() {
		fREC.evaluationWriteEraseResults();
		return Response.ok().build();
	}


}
