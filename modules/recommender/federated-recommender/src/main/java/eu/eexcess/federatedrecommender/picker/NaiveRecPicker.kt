package eu.eexcess.federatedrecommender.picker

import com.aliasi.matrix.SvdMatrix
import eu.eexcess.dataformats.PartnerBadge
import eu.eexcess.dataformats.result.DocumentBadge
import eu.eexcess.dataformats.result.Result
import eu.eexcess.dataformats.result.ResultList
import eu.eexcess.dataformats.userprofile.FeatureVector
import eu.eexcess.dataformats.userprofile.SecureUserProfile
import eu.eexcess.federatedrecommender.dataformats.PFRChronicle
import eu.eexcess.federatedrecommender.dataformats.PartnersFederatedRecommendations
import eu.eexcess.federatedrecommender.interfaces.PartnersFederatedRecommendationsPicker
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


/**
 * Created by hziak on 25.01.16.
 */
class NaiveRecPicker : PartnersFederatedRecommendationsPicker() {
    private val LOGGER = Logger.getLogger("eu.eexcess.federatedRecommender.picker.NaiveRecPicker")




    override fun pickResults(secureUserProfile: SecureUserProfile?, resultList: PartnersFederatedRecommendations?, partners: MutableList<PartnerBadge>?, numResults: Int): ResultList? {
        val combinedResults = ArrayList<Result>()


        var maxSize = 0;
        resultList?.results?.entries?.forEach{ element ->
            element?.value?.results?.forEachIndexed { i, result ->
                if (element.value.results.size>maxSize) maxSize=element.value.results.size
                result.position =  if(element.key.queryGeneratorClass!=null && element.key.queryGeneratorClass.contains("MainTopic")) i.toDouble() else i.toDouble()+1 //Boost if the query is more complex and supports main topic
                result.documentBadge.expertLevel = element.key.expertLevel;
                combinedResults.add(result)
            }
        }
      //  combinedResults.forEach { it.position = maxSize-it.position }



        val termDocumentMatrix = createTermDocMatrix(secureUserProfile, combinedResults)
        var featureMatrix = createFeatureMatrix(termDocumentMatrix, combinedResults)

        var keywordList = String()
        secureUserProfile?.contextKeywords?.forEachIndexed { x, contextKeyword ->
            if (keywordList.length > 0)
                keywordList += "," + contextKeyword.text
            else
                keywordList = contextKeyword.text
        };

        val search = search(featureMatrix, combinedResults, secureUserProfile!!)

        return search;

    }

    private fun search(featureMatrix: Array<out DoubleArray>, combinedResults: ArrayList<Result>, secureUserProfile: SecureUserProfile): ResultList? {
     val dotProductMap = HashMap<Int, Double>()
        var tmpVector = DoubleArray(secureUserProfile.preferences.vector.size + secureUserProfile.contextKeywords.size+1)
        secureUserProfile.contextKeywords.forEachIndexed { i, d ->
            tmpVector[i] = 1.0
        }
        secureUserProfile.preferences.vector.forEachIndexed { i, d ->
            tmpVector[i + secureUserProfile.contextKeywords.size!!] = secureUserProfile.preferences.vector[i]
        }
        tmpVector[secureUserProfile.preferences.vector.size + secureUserProfile.contextKeywords.size]=2.0 //to take the original ranking into account

        for (i in featureMatrix.first().indices) {
            val tmpFeatureMatrix = DoubleArray(featureMatrix.size)
            featureMatrix.forEachIndexed { j, d ->
                tmpFeatureMatrix[j] = featureMatrix[j][i]
            }

            var score = dotProduct(tmpVector, tmpFeatureMatrix)
            dotProductMap.put(i, score)
        }
        var resultList = ResultList()
        var sortedEntries = dotProductMap.entries.sortedByDescending { key -> key.value }
        if (secureUserProfile?.numResults == null)
            secureUserProfile.numResults = 10


        sortedEntries.forEach(
                {
                    if (resultList.results.size < secureUserProfile?.numResults) {
                        resultList.results.add(combinedResults[it.key])
                    } else return resultList
                })
        return resultList
    }

    /**
     * calculates the dot product of two vectors
     */
    internal fun dotProduct(xs: DoubleArray, ys: DoubleArray): Double {
        var sum = 0.0
        for (k in ys.indices)
            try {
                sum += xs[k] * ys[k]
            } catch(e: Exception) {
                LOGGER.log(Level.WARNING, "Index out of Bounce", e)
            }
        return sum
    }



    private fun createTermDocMatrix(secureUserProfile: SecureUserProfile?, combinedResults: ArrayList<Result>): Array<out DoubleArray> {


        val matrix: Array<out DoubleArray> = Array(secureUserProfile?.contextKeywords?.size!!, { DoubleArray(combinedResults.size) })

        secureUserProfile?.contextKeywords?.forEachIndexed { x, contextKeyword ->

            contextKeyword.text.forEach {
                combinedResults.forEachIndexed { y, document ->
                    val combinedTitleDesc = document.title + " " + document.description
                    var counter = 0.0;
                    combinedTitleDesc.split("\\s").forEach { element ->
                        if (element.contains(it)) {
                            if(counter<0.5 && combinedTitleDesc.length>0) {
                               val i = 1.0 / combinedTitleDesc.length
                                counter += i
                            }
                        }
                    }
                    matrix[x][y] +=counter
                }
            }
        }
        return matrix
    }


    fun createFeatureMatrix(oldFeatureMatrix: Array<out DoubleArray>, combinedResults: ArrayList<Result>): Array<out DoubleArray> {
        val matrix: Array<out DoubleArray> = Array(oldFeatureMatrix.size + FeatureVector().vector.size+1, { DoubleArray(oldFeatureMatrix.first().size) })

        for (x in oldFeatureMatrix.indices) {
            oldFeatureMatrix[x].forEachIndexed { y, d ->
                matrix[x][y] = d
            }
        }

        var lastElement = FeatureVector().vector.size + oldFeatureMatrix.size
        FeatureVector().vector.forEachIndexed { i, d ->
            var x = i + oldFeatureMatrix.size
            combinedResults.forEachIndexed { y, document ->

                    when (i) {
                        0 -> if (document.mediaType!=null && document.mediaType.toLowerCase().equals("text", true)) matrix[x][y] = 1.0 else matrix[x][y] = 0.0
                        1 -> if (document.mediaType!=null && document.mediaType.toLowerCase().equals("video", true)) matrix[x][y] = 1.0 else matrix[x][y] = 0.0
                        2 -> if (document.mediaType!=null && document.mediaType.toLowerCase().equals("image", true)) matrix[x][y] = 1.0 else matrix[x][y] = 0.0
                        3 -> if (document.licence != null && !document.licence.toLowerCase().equals("restricted", true)) matrix[x][y] = 1.0 else matrix[x][y] = 0.0
                        4 -> if (document.date != null && !document.date.isEmpty()) matrix[x][y] = 1.0 else matrix[x][y] = 0.0
                        5 -> if (document.documentBadge.expertLevel!=null) matrix[x][y] =  document.documentBadge.expertLevel else matrix[x][y] = 0.0
                    }
                val d1 = Math.pow(Math.log((document.position+1)/combinedResults.size ),2.0)
                matrix[lastElement][y] = d1*1.0


            }

        }
        println()
        matrix.forEachIndexed { x, doubles ->
            doubles.forEachIndexed { i, d ->
                print(" "+d+",")
            }
            println()
        }
        return matrix
    }


    /**
     * Function not implemented
     */
    override fun pickResults(pFRChronicle: PFRChronicle?, numResults: Int): ResultList? {
        throw UnsupportedOperationException()
    }

}
