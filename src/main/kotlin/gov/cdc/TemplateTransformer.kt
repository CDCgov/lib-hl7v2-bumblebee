package gov.cdc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.gson.*
import gov.cdc.hl7.HL7ParseUtils
import gov.cdc.hl7.model.Profile

/**
 * Class to perform transformation from HL7 v.2.x to custom JSON tree based on a JSON template.
 */
class TemplateTransformer(private val template: JsonObject, private val profile: Profile) {
    companion object {
        /**
         * Factory method that returns an instance of TemplateTransformer.
         * Recommended over the use of the constructor for obtaining a class instance.
         * @param configFileName Name of resource file that contains HL7 parsing information
         * @param profileFileName Name of resource file that is the JSON output template
         * @return instance of TemplateTransformer
         */
        @JvmStatic
        fun getTransformerWithResource(configFileName: String, profileFileName: String): TemplateTransformer {
            val templateFile = loadContent(configFileName)
            val jsonbody = JsonParser.parseString(templateFile).asJsonObject
            val mapper = ObjectMapper()
            mapper.registerModule(DefaultScalaModule())
            val profileFile = loadContent(profileFileName)
            val profileObj = mapper.readValue(profileFile, Profile::class.java)
            return TemplateTransformer(jsonbody, profileObj)
        }

        /**
         * Factory method that returns an instance of TemplateTransformer.
         * Recommended over the use of the constructor for obtaining a class instance.
         * @param template the JSON template contents as String
         * @param profile the HL7 profile as String
         * @return an instance of TemplateTransformer
         */
        @JvmStatic
        fun getTransformerWithContent(template: String, profile: String): TemplateTransformer {
            val jsonbody = JsonParser.parseString(template).asJsonObject
            val mapper = ObjectMapper()
            mapper.registerModule(DefaultScalaModule())
            val profileObj = mapper.readValue(profile, Profile::class.java)

            return TemplateTransformer(jsonbody, profileObj)

        }
        private fun loadContent(filePath: String) : String {
            val pathToContent = if (filePath.startsWith("/")) {
                filePath.substringAfter("/")
            } else {
                filePath
            }
            return TemplateTransformer::class.java.getResource("/$pathToContent").readText()
        }
        val INDEXED_VALUE = "\\(([0-9]+)\\)$".toRegex()
        const val DO_NOT_CONCATENATE = "DONT"
    }

    /**
     * Transforms HL7 message to custom JSON format using the default of presenting repeating values as an array
     * @param hl7Message contents of HL7 message as String
     * @return String of JSON data
     */
    fun transformMessage(hl7Message:String) : String {
        return transformMessage(hl7Message, DO_NOT_CONCATENATE)
    }

    /**
     * Transforms HL7 message to custom JSON format
     * @param hl7Message contents of HL7 message as String
     * @param concatArrayElements optional parameter specifying the delimiter to use when concatenating repeating values.
     * If not supplied, repeating values are presented as an array.
     * @return String of JSON data
     */
    fun transformMessage(hl7Message: String, concatArrayElements: String = DO_NOT_CONCATENATE): String {
        val hl7Parser = HL7ParseUtils(hl7Message, profile)
        val copyDoc = template.deepCopy()
        navigateJson(hl7Parser, template, copyDoc = copyDoc, concatArrayElements = concatArrayElements)
        return copyDoc.toString()
    }


    private fun createJsonTree(bline: List<String>, fromAttr: String, leaf: JsonArray):JsonElement  {
        val retJson = JsonObject()
        if (bline.size == 1)
            retJson.add(bline[0], leaf)
        else if (bline[0] != fromAttr)
            return createJsonTree(bline.drop(1), fromAttr, leaf)
        else
            retJson.add(bline[0], createJsonTree(bline.drop(1), bline[1], leaf))
        return retJson
    }

    private fun createJsonTree(bline: List<String>, fromAttr: String,  leafNode: String): JsonElement {
        val retJson = JsonObject()
        if (bline.size == 1)
            retJson.addProperty(bline[0], leafNode)
         else if (bline[0] != fromAttr)
            return createJsonTree(bline.drop(1), fromAttr, leafNode)
         else
            retJson.add(bline[0], createJsonTree(bline.drop(1), bline[1], leafNode))
        return retJson
    }

    private fun mergeNodes(targetNode: JsonObject,  newNode: JsonObject) {
        var targetPointer = targetNode
        var newNodePointer = newNode
        while( targetPointer.has(newNodePointer.keySet().first())) {
            targetPointer = targetPointer.get(newNodePointer.keySet().first()).asJsonObject
            newNodePointer = newNodePointer.get(newNodePointer.keySet().first()).asJsonObject
        }
        targetPointer.add(newNodePointer.keySet().first(), newNodePointer.get(newNodePointer.keySet().first()))
    }

    private fun navigateJson(hl7Message: HL7ParseUtils, elem: JsonElement, parent: JsonElement? = null, path: String = "", attr: String = "", copyDoc: JsonElement, copyDocParent: JsonElement? =null, concatArrayElements: String) {
        if (elem.isJsonObject) {
            (elem as JsonObject).entrySet().map { prop -> navigateJson(hl7Message, prop.value, elem, "$path`${prop.key}", prop.key, (copyDoc as JsonObject).get(prop.key), copyDoc, concatArrayElements) }
        } else if (elem.isJsonArray) {
            val map = mutableMapOf<String, Array<out Array<String>>?>()
            processArray(elem as JsonArray, map, hl7Message, path)
            val newArray = createNewJsonArray(hl7Message, map, attr)
            if (copyDocParent is JsonObject) {
                copyDocParent.remove(attr)
                copyDocParent.add(attr, newArray)
            }

        } else if (elem.isJsonPrimitive) {
            val prim = elem.asJsonPrimitive.asString
            var queryPath = prim
            var resultIndex: Int = -1
            //Check if there's index at then end ... => xxx(9)
            INDEXED_VALUE.find(prim)?.value.let {
                if (it != null) {
                    queryPath = prim.substring(0 until prim.length - it.length)
                    resultIndex = it.substring(1, it.length-1).toInt()
                }
            }
            var newValue: List<String>? = transformVariable(hl7Message, queryPath)?.flatten()
            if (resultIndex >= 0)
                newValue = try { newValue?.slice(resultIndex..resultIndex)  }
                            catch (e: IndexOutOfBoundsException) { null }
            if (!newValue.isNullOrEmpty()) {
                val arrayValues = JsonArray()
                newValue.forEachIndexed { i, it ->
                    if (parent!!.isJsonObject) {
                        if (newValue.size == 1) {
                            (copyDocParent as JsonObject).addProperty(getPropertyName(hl7Message, attr), it)
                            if (attr.startsWith("$$"))
                                (copyDocParent).remove(attr)
                        } else {
                            arrayValues.add(it)
                        }
                    } else if (parent.isJsonArray)
                        (copyDocParent as JsonArray)[i] = JsonPrimitive(it)
                }
                if (arrayValues.size() > 1) {
                    when (concatArrayElements) {
                        DO_NOT_CONCATENATE -> (copyDocParent as JsonObject).add(getPropertyName(hl7Message, attr), arrayValues)
                        else ->  (copyDocParent as JsonObject).addProperty(getPropertyName(hl7Message, attr), arrayValues.joinToString(separator = concatArrayElements) { it.asString })
                    }
                }
            } else {
                if (parent!!.isJsonObject) {
                    (copyDocParent as JsonObject).add(getPropertyName(hl7Message, attr), null)
                }
            }
        }
    }

    private fun getPropertyName(parser: HL7ParseUtils, attr: String): String {
        return if (attr.startsWith("$$") && attr.length > 2) {
            val propName =  transformVariable(parser, attr.substring(2))
            propName?.get(0)!![0]
        } else {
            attr
        }
    }

    private fun processArray(elem: JsonArray, mapValues: MutableMap<String, Array<out Array<String>>?>, hl7Message: HL7ParseUtils, path: String) {
        elem.forEach { prop ->
            populateArrayProperties(prop, mapValues, hl7Message, path)
        }
    }

    private fun populateArrayProperties(prop: JsonElement, map: MutableMap<String, Array<out Array<String>>?>, hl7Message: HL7ParseUtils, path: String) {
        if (prop.isJsonObject) {
            (prop as JsonObject).entrySet().map{ pp ->
                if (pp.value.isJsonPrimitive) {
                        transformVariable(hl7Message, pp.value.asString)?.let { map.put("$path`${pp.key}", it) }
                } else if (pp.value.isJsonObject) {
                    populateArrayProperties(pp.value, map, hl7Message, "$path`${pp.key}")
                } else if (pp.value.isJsonArray) {
                   throw UnsupportedOperationException("Unable to parse Arrays of arrays")
                } else {
                    println("Not sure: ${pp.value.javaClass}")
                }

            }
        } else {
            println("prop is ${prop.javaClass}")
        }
    }

    private fun createNewJsonArray(hl7Message: HL7ParseUtils, mapValues: MutableMap<String, Array<out Array<String>>?>, attr: String): JsonArray {
        //Add all Elements to array:
        val newArray = JsonArray()
        for (index in 1..(mapValues.values.first()?.size ?: 0)) {
            val newObj = JsonObject()
            mapValues.forEach {  k ->
                val kSize = k.value?.size ?: 0
                val innerValue = if (kSize >= index) k.value?.get(index - 1) else null
                if (innerValue == null)
                    newObj.add(getPropertyName(hl7Message, k.key), null)
                else when (innerValue.size) {
                    0 -> newObj.add(getPropertyName(hl7Message, k.key.substring(1).split('`').last()), null)
                    1 -> {
                        val bline = k.key.split('`').map{ getPropertyName(hl7Message, it.replace("[*]", "[${index-1}]"))}
                        val tempJsonObj = createJsonTree(bline, attr, innerValue[0])
                        mergeNodes(newObj, tempJsonObj as JsonObject)
                        //newObj.
                    }
                    else -> {
                        val newInnerArray = JsonArray()
                        innerValue.forEach { r -> newInnerArray.add(r) }
                        val bline = k.key.split('`').map{ getPropertyName(hl7Message, it)}
                        val arrayWrapper = JsonObject()
                        arrayWrapper.add(getPropertyName(hl7Message, attr), newInnerArray)
                        val tempJsonObj = createJsonTree(bline, getPropertyName(hl7Message, attr), newInnerArray)
                        mergeNodes(newObj, tempJsonObj as JsonObject)
                    }
                }
            }
            newArray.add(newObj.get(getPropertyName(hl7Message, attr)))

        }
        return newArray

    }
    private fun transformVariable(hl7Parser: HL7ParseUtils,  prim: String?): Array<out Array<String>>? {
            val optionalValue = hl7Parser.getValue(prim, false) //Don't remove empties
            if (optionalValue.isDefined)
                return optionalValue.get()
            return null
    }
}



