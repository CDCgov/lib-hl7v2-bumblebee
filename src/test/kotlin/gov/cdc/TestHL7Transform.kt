package gov.cdc


import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class TestHL7Transform {
    @Test
    fun testHL7Transformer() {
        val message = this::class.java.getResource("/testMessage.txt").readText()
        val gson = GsonBuilder().create()
        val xformer = HL7JsonTransformer.getTransformerWithResource(message,
            "PhinProfile.json",
            "DefaultFieldsProfileSimple.json")
        val fullHL7 = xformer.transformMessage()
        println(gson.toJson(fullHL7))
        assertTrue(fullHL7.get("MSH").asJsonObject
            .get("receiving_facility").asJsonObject
            .get("namespace_id").asString == "NCIRD-VPD")
    }
    @Test
    fun testHL7TransformerGenerated() {
        val message = this::class.java.getResource("/COVID.txt").readText()
        val gson = GsonBuilder().create()
        val xformer = HL7JsonTransformer.getTransformerWithResource(message,
            "profile-covid19_elr_v2_5_new.json",
            "fields-covid19_elr_v2_5_new.json")
        val fullHL7 = xformer.transformMessage()
        println(gson.toJson(fullHL7))
    }

    @Test
    fun loadFieldDef() {
        val content = this::class.java.getResource("/DefaultFieldsProfileSimple.json").readText()

        val gson = Gson()

        val profile: Profile = gson.fromJson(content, Profile::class.java)
        assertTrue(profile.getSegmentField("HD")?.size == 3)
        println((profile.getSegmentField("HD")?.get(0))?.name)
        println((profile.getSegmentField("HD")?.get(1))?.name)
        println((profile.getSegmentField("HD")?.get(2))?.name)
        println(profile.segmentFields.keys)
    }

    @Test
    fun testRemoveParens() {
        val regex = "\\(([0-9]+)\\)$".toRegex()
        val testString = "OBX[@3.1='PLT631||PLT656']-5.1(10)"
        regex.find(testString)?.value.let {
            if (it != null) {
                println(it)
                println(it.substring(1, it.length-1).toInt())
                println(testString.substring(0 until testString.length-it.length))
            }

        }
    }
}