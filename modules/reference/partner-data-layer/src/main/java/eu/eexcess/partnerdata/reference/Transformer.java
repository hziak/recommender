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
package eu.eexcess.partnerdata.reference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.eexcess.config.PartnerConfiguration;
import eu.eexcess.dataformats.result.Result;
import eu.eexcess.dataformats.result.ResultList;
import eu.eexcess.partnerdata.api.EEXCESSDataTransformationException;
import eu.eexcess.partnerdata.api.ITransformer;

public class Transformer implements ITransformer {

    private static final String EEXCESS_FACETS_VALUE_DEFAULT_LICENCE = "restricted";
    protected static final String EEXCESS_FACETS_VALUE_UNKNOWN = "unknown";
    protected static final String EEXCESS_MEDIATYPE_TEXT = "text";
    protected PartnerConfiguration partnerConfig;
    protected String resultListTransformationFilename;

    protected String resultObjectTransformationFilename;

    protected javax.xml.transform.Transformer transformerResultList;
    protected javax.xml.transform.Transformer transformerResultObject;

    protected Document transformationResultList;
    protected Document transformationResultObject;
    protected final Logger log = Logger.getLogger(Transformer.class.getName());
    /*
     * protected String xslTranformationRemoveEmptyElements =
     * "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
     * + " <xsl:strip-space elements=\"*\"/> " +
     * "<xsl:output indent=\"yes\" /> " + "<xsl:template match=\"@*|node()\">" +
     * "<xsl:if test=\". != '' or ./@* != ''\">" + "<xsl:copy>" +
     * "<xsl:apply-templates  select=\"@*|node()\"/> " + "</xsl:copy>" +
     * "</xsl:if>" + "</xsl:template>" + "</xsl:stylesheet>";
     */
    protected String xslTranformationRemoveEmptyElements = "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" + "<xsl:template match=\"*\">"
            + "  <xsl:if test=\"normalize-space(.) != ''\"> " + "    <xsl:copy>" + "      <xsl:apply-templates/>" + "    </xsl:copy>" + "  </xsl:if>" + "</xsl:template>"
            + "</xsl:stylesheet>";
    protected Query sparqlQueryForToResultList;
    protected String sparqlQueryForToResultListString;

    @Override
    public boolean hasEEXCESSRDFResponseResults(Document eexcessResults) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xPath.evaluate("//*[local-name()='Proxy']", eexcessResults.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength();) {
                return true;
            }
        } catch (XPathExpressionException e1) {
            e1.printStackTrace();
        }
        return false;

    }

    @Override
    public void init(PartnerConfiguration partnerConfig) throws TransformerConfigurationException {
        this.partnerConfig = partnerConfig;
        this.resultListTransformationFilename = partnerConfig.getMappingListTransformationFile();
        this.resultObjectTransformationFilename = partnerConfig.getMappingObjectTransformationFile();

        TransformerFactory tFactory = TransformerFactory.newInstance();
        InputStream resultListTransformationStream = this.getClass().getResourceAsStream("/" + this.resultListTransformationFilename);
        transformerResultList = tFactory.newTransformer(new StreamSource(resultListTransformationStream));
        InputStream resultObjectTransformationStream = this.getClass().getResourceAsStream("/" + this.resultObjectTransformationFilename);
        transformerResultObject = tFactory.newTransformer(new StreamSource(resultObjectTransformationStream));

        try {
            InputStream resultListTransformationFileStream = this.getClass().getResourceAsStream("/" + this.resultListTransformationFilename);
            InputStreamReader in = new InputStreamReader(resultListTransformationFileStream);
            BufferedReader reader = new BufferedReader(in);

            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            resultListTransformationFileStream.close();
            resultListTransformationFileStream = null;
            in.close();
            in = null;
            reader.close();
            reader = null;
            PartnerdataTracer.debugTrace(this.partnerConfig, "Transformation:" + out.toString());
            transformationResultList = XMLTools.convertStringToDocument(out.toString());

            InputStream resourceAsStream = this.getClass().getResourceAsStream("/" + this.resultObjectTransformationFilename);
            InputStreamReader isReader = new InputStreamReader(resourceAsStream);
            reader = new BufferedReader(isReader);
            out = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            transformationResultObject = XMLTools.convertStringToDocument(out.toString());
            reader.close();
            isReader.close();
            resourceAsStream.close();
            reader = null;
            isReader = null;
            resourceAsStream = null;
        } catch (IOException e) {

            e.printStackTrace();
            throw new TransformerConfigurationException(e);
        } finally {
            try {
                resultListTransformationStream.close();
                resultObjectTransformationStream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        // model = ModelFactory.createOntologyModel();

        sparqlQueryForToResultListString = createSPARQLqueryForToResultList();

        sparqlQueryForToResultList = QueryFactory.create(sparqlQueryForToResultListString);

    }

    protected Document transformInternal(Document input, javax.xml.transform.Transformer transformer, PartnerdataLogger logger) throws EEXCESSDataTransformationException {
        PartnerdataTracer.debugTrace(this.partnerConfig, "before transform:\n" + XMLTools.getStringFromDocument(input));

        PartnerdataTracer.dumpFile(this.getClass(), partnerConfig, input, "before-transform", logger);

        input = preProcessTransform(input, logger);

        DOMSource source = new DOMSource(input);
        DOMResult domResultCleaned = new DOMResult();
        DOMResult domResult = new DOMResult();
        try {
            // remove all empty Tags
            TransformerFactory tFactory = TransformerFactory.newInstance();
            if (this.partnerConfig.getMakeCleanupBeforeTransformation()) {
                javax.xml.transform.Transformer transformerRemoveEmptyTags = tFactory.newTransformer(new StreamSource(new StringReader(xslTranformationRemoveEmptyElements)));
                transformerRemoveEmptyTags.transform(source, domResultCleaned);
                PartnerdataTracer.dumpFile(this.getClass(), partnerConfig, (Document) domResultCleaned.getNode(), "before-transform-after-cleanup", logger);
                transformer.transform(new DOMSource(domResultCleaned.getNode()), domResult);
            } else {
                transformer.transform(source, domResult);
            }
            Document result = (Document) domResult.getNode();
            result = this.postTransformationResults(input, result);
            PartnerdataTracer.debugTrace(this.partnerConfig, "after transform:\n" + XMLTools.getStringFromDocument(result));
            PartnerdataTracer.dumpFile(this.getClass(), partnerConfig, result, "done-transform", logger);
            return result;
        } catch (TransformerException e) {
            throw new EEXCESSDataTransformationException(e);
        }
    }

    protected Document transformInternalDetail(Document input, javax.xml.transform.Transformer transformer, PartnerdataLogger logger) throws EEXCESSDataTransformationException {
        PartnerdataTracer.debugTrace(this.partnerConfig, "before transform-detail:\n" + XMLTools.getStringFromDocument(input));

        PartnerdataTracer.dumpFile(this.getClass(), partnerConfig, input, "before-transform-detail", logger);

        input = preProcessTransformDetail(input, logger);

        DOMSource source = new DOMSource(input);
        DOMResult domResult = new DOMResult();
        try {
            transformer.transform(source, domResult);
            Document result = (Document) domResult.getNode();
            result = this.postTransformationResultsDetail(input, result);
            PartnerdataTracer.debugTrace(this.partnerConfig, "after transform-detail:\n" + XMLTools.getStringFromDocument(result));
            PartnerdataTracer.dumpFile(this.getClass(), partnerConfig, result, "done-transform-detail", logger);
            return result;
        } catch (TransformerException e) {
            throw new EEXCESSDataTransformationException(e);
        }
    }

    @Override
    public Document transform(Document input, PartnerdataLogger logger) throws EEXCESSDataTransformationException {
        return transformInternal(input, transformerResultList, logger);
    }

    @Override
    public Document transformDetail(Document input, PartnerdataLogger logger) throws EEXCESSDataTransformationException {
        return transformInternalDetail(input, transformerResultObject, logger);
    }

    public Document preProcessTransform(Document input, PartnerdataLogger logger) throws EEXCESSDataTransformationException {
        return input;
    }

    public Document preProcessTransformDetail(Document input, PartnerdataLogger logger) throws EEXCESSDataTransformationException {
        return input;
    }

    @Override
    public ResultList toResultList(Document nativeResults, Document input, PartnerdataLogger logger) {
        ResultList returnList = new ResultList();

        // model.removeAll();
        // OntModel tmpModel =new OntModelImpl(model.getSpecification(),model);
        OntModel model = ModelFactory.createOntologyModel();
        String inputString = XMLTools.getStringFromDocument(input);
        // System.out.println(inputString);
        StringReader stream = new StringReader(inputString);
        model.read(stream, null);

        if (this.partnerConfig.getPartnerDataRequestsTrace()) {// for debugging:
            PartnerdataTracer.debugTrace(this.partnerConfig, "createSPARQLqueryForToResultList:\n" + sparqlQueryForToResultListString);
            Query queryDebug = QueryFactory.create(sparqlQueryForToResultListString);
            QueryExecution qeDebug = QueryExecutionFactory.create(queryDebug, model);
            ResultSet queryResultsDebug = qeDebug.execSelect();
            log.info("createSPARQLqueryForToResultList Result:\n" + ResultSetFormatter.asText(queryResultsDebug));
            qeDebug.close();

        }
        QueryExecution qe = QueryExecutionFactory.create(sparqlQueryForToResultList, model);

        ResultSet queryResults = qe.execSelect();

        LinkedList<Result> resultsList = new LinkedList<Result>();

        while (queryResults.hasNext()) {

            QuerySolution querySol = queryResults.nextSolution();
            // Resource proxyUri = querySol.getResource("proxyUri");

            Result result = new Result();

            /*
             * Resource edmAggregatedCHO =
             * querySol.getResource("edmAggregatedCHO"); if (edmAggregatedCHO !=
             * null) { result.eexcessURI = edmAggregatedCHO.toString(); }
             */
            // TODO if result.eexcessURI is empty skip this entry
            Literal proxyIdentifier = querySol.getLiteral("proxyIdentifier");
            if (proxyIdentifier != null)
                result.documentBadge.id = proxyIdentifier.toString();

            Literal proxyTitle = querySol.getLiteral("proxyTitle");
            if (proxyTitle != null)
                result.title = proxyTitle.toString().trim();

            Literal proxyDescription = querySol.getLiteral("proxyDescription");
            if (proxyDescription != null)
                result.description = proxyDescription.toString();

            /*
             * Literal proxyCreator = querySol.getLiteral("proxyCreator"); if
             * (proxyCreator != null) result.creator = proxyCreator.toString();
             */
            Literal dctermsDate = querySol.getLiteral("date");
            if (dctermsDate != null)
                result.date = dctermsDate.toString();

            Literal edmProviderName = querySol.getLiteral("edmProviderName");
            if (edmProviderName != null) {
                result.documentBadge.provider = edmProviderName.toString().trim();
            }

            Literal proxyLanguage = querySol.getLiteral("proxyLanguage");
            if (proxyLanguage != null) {
                result.language = proxyLanguage.toString().trim();
            }

            Literal proxyType = querySol.getLiteral("proxyType");
            if (proxyType != null) {
                result.mediaType = proxyType.toString();
            }

            Literal proxyRights = querySol.getLiteral("proxyRights");
            if (proxyRights != null) {
                result.licence = proxyRights.toString().trim();
            } else {
                result.licence = EEXCESS_FACETS_VALUE_DEFAULT_LICENCE;
            }

            Resource oreIsShownAt = querySol.getResource("oreIsShownAt");
            if (oreIsShownAt != null) {
                result.documentBadge.uri = oreIsShownAt.toString();
            }
            Resource edmPreview = querySol.getResource("edmPreview");
            if (edmPreview != null) {
                String xmlBase = this.getValueWithXPath("//*[contains(@name,'xml:base')]", this.transformationResultList); // [@name='xml:base']
                if (!edmPreview.toString().equalsIgnoreCase(xmlBase))
                    result.previewImage = edmPreview.toString();
            }
            // fillup empty values
            if (result.language == null || result.language.isEmpty() || result.language.trim().isEmpty()) {
                result.language = EEXCESS_FACETS_VALUE_UNKNOWN;
            }
            if (result.licence == null || result.licence.isEmpty() || result.licence.trim().isEmpty()) {
                result.licence = EEXCESS_FACETS_VALUE_UNKNOWN;
            }
            if (result.documentBadge.provider == null || result.documentBadge.provider.isEmpty() || result.documentBadge.provider.trim().isEmpty()) {
                result.documentBadge.provider = EEXCESS_FACETS_VALUE_UNKNOWN;
            }
            if (result.mediaType == null || result.mediaType.isEmpty() || result.mediaType.trim().isEmpty()) {
                result.mediaType = EEXCESS_FACETS_VALUE_UNKNOWN;
            }
            if (result.date == null || result.date.isEmpty() || result.date.trim().isEmpty()) {
                result.date = EEXCESS_FACETS_VALUE_UNKNOWN;
            } else {
            	result.date = parseDate(result.date);
            }

            result = postProcessResult(input, result, querySol);
            resultsList.add(result);
        }

        returnList.results = resultsList;
        returnList = postProcessResults(nativeResults, returnList);
        return returnList;
    }

    
	private String parseDate(String strDate) {
		SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
		boolean dateFormatDetected = false;
		ArrayList<String> inputFormatList = new ArrayList<String>();
		inputFormatList.add("yyyy-MM-dd");
		inputFormatList.add("yyyy-MM");
		inputFormatList.add("yyyy");
		Date javaDate = null;
		
		for (int i = 0; i < inputFormatList.size(); i++) {
			SimpleDateFormat inputFormat = new SimpleDateFormat(inputFormatList.get(i));
			inputFormat.setLenient(true);
			try {
				javaDate = inputFormat.parse(strDate);
				dateFormatDetected = true;
				break;
			}
			catch (ParseException e) {
				dateFormatDetected = false;
			}
		}
		String output=strDate;
		if (dateFormatDetected) 
			output = outputFormat.format(javaDate);
		return output;
	}

	/*
     * private Object extractRDFForOneObject(Result result, String inputString)
     * {
     * 
     * String queryString = getRDFPrefixes() + "DESCRIBE ?x " + "WHERE {" // +
     * "{?x ?y <" + result.eexcessURI + "> }" + + "{?x ?y <" +
     * result.documentBadge.uri + "> }" + " }" ;
     * 
     * model.removeAll(); OntModel tmpModel =new
     * OntModelImpl(model.getSpecification(),model);
     * 
     * com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString);
     * 
     * QueryExecution qe = QueryExecutionFactory.create(query, tmpModel);
     * 
     * if (this.partnerConfig.partnerDataRequestsTrace) {// for debugging:
     * PartnerdataTracer.debugTrace(this.partnerConfig, queryString); Query
     * queryDebug = QueryFactory.create(queryString); QueryExecution qeDebug =
     * QueryExecutionFactory.create(queryDebug, tmpModel); Model
     * queryResultsDebug = qeDebug.execDescribe();
     * PartnerdataTracer.debugTrace(this.partnerConfig,
     * XMLTools.writeModel(queryResultsDebug));
     * 
     * 
     * }
     * 
     * Model newRawModel = qe.execDescribe(); qe.close(); return
     * XMLTools.writeModel(newRawModel); }
     */
    protected Result postProcessResult(Document orgPartnerResult, Result result, QuerySolution querySol) {
        return result;
    }

    protected Document postTransformationResults(Document orgPartnerResults, Document transformedResults) {
        return transformedResults;
    }

    protected Document postTransformationResultsDetail(Document orgPartnerResults, Document transformedResults) {
        return transformedResults;
    }

    protected ResultList postProcessResults(Document orgPartnerResult, ResultList resultList) {
        return resultList;
    }

    protected String createSPARQLqueryForToResultList() {
        String queryContent = "";
        queryContent += getRDFPrefixes();

        queryContent += "SELECT ?edmAggregatedCHO ?proxyIdentifier ?proxyTitle ?oreIsShownAt ?proxyDescription ?proxyCreator ?edmProviderName ?oreCollectionName ?proxyLanguage ?proxyType ?proxyRights ?date ?edmPreview ";
        queryContent += "WHERE {  ";
        queryContent += "?eexcessProxy rdf:type eexcess:Proxy . ";
        queryContent += "?eexcessProxy ore:proxyIn ?oreAggregation . ";
        queryContent += "?oreAggregation rdf:type ore:Aggregation . ";
        queryContent += "?oreAggregation edm:provider ?edmProvider . ";
        queryContent += "?edmProvider foaf:name ?edmProviderName . ";
        queryContent += "?oreAggregation edm:aggregatedCHO ?edmAggregatedCHO . ";
        queryContent += "OPTIONAL { ?eexcessProxy dc:identifier ?proxyIdentifier . } ";
        queryContent += "OPTIONAL { ?eexcessProxy dc:title ?proxyTitle . }  ";
        queryContent += "OPTIONAL { ?eexcessProxy dc:description ?proxyDescription . }  ";
        queryContent += "OPTIONAL { ?eexcessProxy dcterms:date ?date . }  ";
        queryContent += "OPTIONAL { ?eexcessProxy edm:language ?proxyLanguage . }  ";
        queryContent += "OPTIONAL { ?eexcessProxy edm:type ?proxyType . }  ";
        queryContent += "OPTIONAL { ?eexcessProxy edm:rights ?proxyRights . }  ";
        queryContent += "OPTIONAL { ?oreAggregation edm:isShownAt ?oreIsShownAt . } ";
        queryContent += "OPTIONAL { ?oreAggregation edm:preview ?edmPreview . } ";
        queryContent += "OPTIONAL { ?oreAggregation edm:collectionName ?oreCollectionName . } ";

        queryContent += "OPTIONAL { ?eexcessProxy dc:creator ?proxyCreator . }  ";

        queryContent += " } ";

        return queryContent;
    }

    private String getRDFPrefixes() {
        String queryContent = "";
        queryContent += "PREFIX eexcess: <http://eexcess.eu/schema/> ";
        queryContent += "PREFIX dcterms: <http://purl.org/dc/terms/> ";
        queryContent += "PREFIX dc: <http://purl.org/dc/elements/1.1/> ";
        queryContent += "PREFIX ore: <http://www.openarchives.org/ore/terms/> ";
        queryContent += "PREFIX edm: <http://www.europeana.eu/schemas/edm/> ";
        queryContent += "PREFIX foaf: <http://xmlns.com/foaf/0.1/> ";
        queryContent += "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ";
        queryContent += "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ";
        queryContent += "PREFIX dbpedia: <http://dbpedia.org/ontology/> ";
        return queryContent;
    }

    protected String getValueWithXPath(String xpath, Document orgPartnerResult) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xPath.evaluate(xpath, orgPartnerResult.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength();) {
                Element e = (Element) nodes.item(i);
                return e.getTextContent();
            }
        } catch (XPathExpressionException e1) {
            e1.printStackTrace();
        }

        return "";
    }

    protected String getAttributeWithXPath(String xpath, Document orgPartnerResult) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodes;
        try {
            nodes = (NodeList) xPath.evaluate(xpath, orgPartnerResult.getDocumentElement(), XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength();) {
                Node e = nodes.item(i);
                return e.getNodeValue();
            }
        } catch (XPathExpressionException e1) {
            e1.printStackTrace();
        }

        return "";
    }

    protected String getRootAttribute(String attribut, Document orgPartnerResult, ResultList resultList) {
        Element element = orgPartnerResult.getDocumentElement();
        return element.getAttribute(attribut);
    }

    
	public Document removeNameSpaces(Document doc) {
	    NodeList list = doc.getChildNodes();
	    for (int i = 0; i < list.getLength(); i++) {
	        removeNameSpaces(list.item(i), "");
	    }

	    return doc;
	}
	
	private void removeNameSpaces(Node node, String nameSpaceURI) {
	    if (node.getNodeType() == Node.ELEMENT_NODE) {
	        Document ownerDoc = node.getOwnerDocument();
	        NamedNodeMap map = node.getAttributes();
	        Node n;
	        while (!(0==map.getLength())) {
	            n = map.item(0);
	            map.removeNamedItemNS(n.getNamespaceURI(), n.getLocalName());
	        }
	        ownerDoc.renameNode(node, nameSpaceURI, node.getLocalName());
	    }
	    NodeList list = node.getChildNodes();
	    for (int i = 0; i < list.getLength(); i++) {
	        removeNameSpaces(list.item(i), nameSpaceURI);
	    }
	}

}
