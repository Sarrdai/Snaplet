package app.tinycam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Every language in res/values-<lang> must translate exactly the translatable keys of the English default. */
class TranslationsTest {
    private val res = File("src/main/res")

    private fun keys(file: File): Map<String, Boolean> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = doc.documentElement
        return (0 until root.childNodes.length).map { root.childNodes.item(it) }.filterIsInstance<Element>()
            .filter { it.tagName == "string" || it.tagName == "plurals" }
            .associate { "${it.tagName}/${it.getAttribute("name")}" to (it.getAttribute("translatable") != "false") }
    }

    @Test
    fun allLanguagesAreComplete() {
        val default = keys(File(res, "values/strings.xml"))
        val translatable = default.filterValues { it }.keys
        val languages = res.listFiles { f -> f.name.startsWith("values-") && File(f, "strings.xml").exists() }.orEmpty()
        assertTrue("No translations found", languages.isNotEmpty())
        for (dir in languages) {
            val translated = keys(File(dir, "strings.xml")).keys
            assertEquals("Missing in ${dir.name}", emptySet<String>(), translatable - translated)
            assertEquals("Unknown or non-translatable keys in ${dir.name}", emptySet<String>(), translated - translatable)
        }
    }
}
