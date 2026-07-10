package com.example.ufitoolsremote

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class ManifestSecurityTest {
    private val manifest by lazy { parseXml(sourceFile("src/main/AndroidManifest.xml")) }

    @Test
    fun widgetCustomActions_areHandledOnlyByNonExportedReceiver() {
        val provider = component("receiver", ".widget.UfiRemoteWidgetProvider")
        val actionReceiver = component("receiver", ".widget.WidgetActionReceiver")

        assertEquals("true", provider.getAttribute("android:exported"))
        assertEquals(
            listOf("android.appwidget.action.APPWIDGET_UPDATE"),
            childElements(provider, "action").map { it.getAttribute("android:name") }
        )
        assertEquals("false", actionReceiver.getAttribute("android:exported"))
        assertTrue(childElements(actionReceiver, "intent-filter").isEmpty())
    }

    @Test
    fun easyTier_usesSpecialUseForegroundServiceDeclaration() {
        val service = component("service", ".easytier.EasyTierService")
        val permissions = childElements(manifest.documentElement, "uses-permission")
            .map { it.getAttribute("android:name") }

        assertEquals("specialUse", service.getAttribute("android:foregroundServiceType"))
        assertTrue(permissions.contains("android.permission.FOREGROUND_SERVICE_SPECIAL_USE"))
        assertFalse(permissions.contains("android.permission.FOREGROUND_SERVICE_DATA_SYNC"))
        assertNotNull(
            childElements(service, "property").singleOrNull {
                it.getAttribute("android:name") == "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" &&
                    it.getAttribute("android:value").isNotBlank()
            }
        )
    }

    @Test
    fun sensitivePreferences_areExcludedFromBothBackupRuleFormats() {
        val application = childElements(manifest.documentElement, "application").single()
        assertEquals("@xml/backup_rules", application.getAttribute("android:fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.getAttribute("android:dataExtractionRules"))

        val legacy = parseXml(sourceFile("src/main/res/xml/backup_rules.xml"))
        assertEquals(SENSITIVE_PREFS, excludedSharedPreferences(legacy))

        val extraction = parseXml(sourceFile("src/main/res/xml/data_extraction_rules.xml"))
        val cloudBackup = childElements(extraction.documentElement, "cloud-backup").single()
        val deviceTransfer = childElements(extraction.documentElement, "device-transfer").single()
        assertEquals(SENSITIVE_PREFS, excludedSharedPreferences(cloudBackup))
        assertEquals(SENSITIVE_PREFS, excludedSharedPreferences(deviceTransfer))
    }

    private fun component(tag: String, name: String): Element {
        return childElements(manifest.documentElement, tag).single {
            it.getAttribute("android:name") == name
        }
    }

    private fun excludedSharedPreferences(document: Document): Set<String> {
        return excludedSharedPreferences(document.documentElement)
    }

    private fun excludedSharedPreferences(element: Element): Set<String> {
        return childElements(element, "exclude")
            .filter { it.getAttribute("domain") == "sharedpref" }
            .map { it.getAttribute("path") }
            .toSet()
    }

    private fun childElements(parent: Element, tag: String): List<Element> {
        val matches = mutableListOf<Element>()
        val nodes = parent.getElementsByTagName(tag)
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            matches += element
        }
        return matches
    }

    private fun parseXml(file: File): Document {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    }

    private fun sourceFile(path: String): File {
        return sequenceOf(File(path), File("app", path)).firstOrNull(File::isFile)
            ?: error("Unable to find source file: $path")
    }

    private companion object {
        val SENSITIVE_PREFS = setOf("ufi_remote_settings.xml", "ufi_remote_sms_cache.xml")
    }
}
