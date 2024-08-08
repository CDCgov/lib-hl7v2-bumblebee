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

        val xformer = HL7JsonTransformer.getTransformerWithResource(message, "PhinGuideProfile.json")
        val fullHL7 = xformer.transformMessage()
        println(gson.toJson(fullHL7))
        assertTrue(fullHL7.get("MSH").asJsonObject
            .get("receiving_facility").asJsonObject
            .get("namespace_id").asString == "NCIRD-VPD")
    }


    @Test
    fun loadFieldDef() {
        val content = this::class.java.getResource("/DefaultFieldsProfile.json").readText()

        val gson = Gson()

        val profile: Profile = gson.fromJson(content, Profile::class.java)
        assertTrue(profile.getSegmentField("HD")?.size == 3)
        println((profile.getSegmentField("HD")?.get(0))?.name)
        println((profile.getSegmentField("HD")?.get(1))?.name)
        println((profile.getSegmentField("HD")?.get(2))?.name)
        println(profile.segmentFields.keys)
    }
}