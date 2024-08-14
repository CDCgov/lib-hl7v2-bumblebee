package gov.cdc

import org.junit.jupiter.api.Test

class TestTemplateTransformer {
    @Test
    fun testSimpleTemplate() {
        val templateTransformer = TemplateTransformer.getTransformerWithResource("/simpleTemplate.json",
        "BasicProfile.json")
        val message = this::class.java.getResource("/exampleHL7Message.txt").readText()
        val output = templateTransformer.transformMessage(message)
        println(output)
    }
}