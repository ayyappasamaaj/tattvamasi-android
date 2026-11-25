package com.ayyappasamaaj.tattvamasi.ui.register

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/** Registration forms as staging describes them (Nov 25: Kids Ganesha Puja 2027 and Mandalam 2026). */
class RegisterFormTest {
    private fun sample(name: String) = parseForm(javaClass.classLoader!!.getResource(name)!!.readText())!!
    private val kids get() = sample("register-kids-ganesha-puja.json")
    private val mandalam get() = sample("register-mandalam.json")

    @Test
    fun theSlugComesFromTheRegistrationLink() {
        assertEquals("mandalam-2026", formSlug("https://ayyappasamaaj.org/forms/mandalam-2026/"))
        assertEquals("kids-ganesha-puja-2027", formSlug("https://ayyappasamaaj.org/forms/kids-ganesha-puja-2027"))
        assertNull(formSlug("https://ayyappasamaaj.org/events/register/annual-picnic/"))
    }

    @Test
    fun theFormIsRead() {
        val form = kids
        assertEquals("Kids Ganesha Puja 2027", form.title)
        assertEquals("required", form.phone)
        assertTrue(form.askWhatsapp && form.offerFeeCover && drawable(form))
        val child = form.items.first { it.id == 57 }
        assertEquals("quantity", child.kind)
        assertEquals(BigDecimal("35.00"), child.price)
        assertEquals("Child's name", child.detailLabel)
        assertEquals(4, child.most)
        assertEquals(listOf("27", "54", "108", "252", "504", "1008"), form.items.first { it.id == 59 }.amounts)
        assertEquals(listOf("Kids Ganesha Puja", "General"), form.groups.map { it.name })
        assertEquals("textarea", form.questions.single().kind)
    }

    @Test
    fun theTotalIsTheWebsites() {
        val form = kids
        val answers = mapOf("item_57" to "2", "item_59" to "54", "item_60" to "true")
        assertEquals(BigDecimal("376.00"), totals(form, answers).total)  // 2 × 35 + 54 + 252
        val covered = totals(form, answers + ("cover_fee" to "true"))
        // The website's processing_fee gives 11.54 for 376 and 1.92 for 54 (checked Nov 25)
        assertEquals(BigDecimal("11.54"), covered.fee)
        assertEquals(BigDecimal("387.54"), covered.total)
        assertEquals(BigDecimal("1.92"), processingFee(form, BigDecimal("54.00")))
    }

    @Test
    fun theAnswersGoUnderTheWebFormsNames() {
        val form = kids
        val json = answersJson(form, mapOf("name" to " Lakshmi ", "email" to "l@example.com", "phone" to "408-555-0101",
            "phone_on_whatsapp" to "true", "item_57" to "2", "item_57_detail_1" to "Anu", "item_57_detail_2" to "Ravi",
            "item_57_detail_3" to "left over", "item_60" to "true", "field_9" to "Vegetarian"))
        assertEquals("Lakshmi", json.getString("name"))
        assertEquals(true, json.getBoolean("phone_on_whatsapp"))
        assertFalse(json.has("whatsapp"))
        assertEquals(2, json.getInt("item_57"))
        assertEquals("Ravi", json.getString("item_57_detail_2"))
        assertFalse(json.has("item_57_detail_3"))  // only as many as chosen
        assertEquals(true, json.getBoolean("item_60"))
        assertEquals(false, json.getBoolean("item_58"))
        assertEquals("", json.getString("item_59"))
        assertEquals(false, json.getBoolean("cover_fee"))
        assertEquals("Vegetarian", json.getString("field_9"))
        val other = answersJson(form, mapOf("phone_on_whatsapp" to "false", "whatsapp" to "+91 98450 00000"))
        assertEquals("+91 98450 00000", other.getString("whatsapp"))
    }

    @Test
    fun questionsRequiredWithAnItem() {
        val form = mandalam
        val date = form.questions.first { it.id == 3 }  // Date for Mala Dharana, required with items 8 and 10
        assertFalse(needed(date, form, emptyMap()))
        assertTrue(needed(date, form, mapOf("item_8" to "1")))
        assertTrue(shown(date, form, emptyMap()))
        val hidden = date.copy(onlyWhenChosen = true)
        assertFalse(shown(hidden, form, emptyMap()))
        assertTrue(shown(hidden, form, mapOf("item_10" to "2")))
    }

    @Test
    fun whatTheWebsiteAnswers() {
        val pay = parseSubmitted(200, """{"status": "pay", "code": "sDEnad7", "token": "6d0b96cb", "total": "54.00",
            "client_secret": "pi_x_secret_y", "publishable_key": "pk_test_x", "hold_minutes": 30}""")
        assertEquals(Submitted.Pay("sDEnad7", "6d0b96cb", "54.00", "pi_x_secret_y", "pk_test_x", 30), pay)
        assertEquals(Submitted.Confirmed("ABC1234", "t", "0.00"),
            parseSubmitted(200, """{"status": "confirmed", "code": "ABC1234", "token": "t", "total": "0.00"}"""))
        // Staging's answer to a missing required question, Nov 25
        val problems = parseSubmitted(400, """{"errors": {"field_2": ["This field is required."]}}""") as Submitted.Problems
        assertEquals(listOf("This field is required."), problems.errors["field_2"])
        assertEquals(Submitted.Refused("This registration is full."),
            parseSubmitted(409, """{"detail": "This registration is full."}"""))
        assertTrue((parseSubmitted(0, "") as Submitted.Refused).message.startsWith("Couldn't reach"))
    }

    @Test
    fun theThankYou() {
        // Staging's answer after the test payment, Nov 25
        val status = parseStatus("""{"code": "sDEnad7", "status": "paid", "confirmed": true, "total": "54.00",
            "title": "Mandala Mahotsavam 2026-27", "thank_you": "Please join the Mandalam 2026 WhatsApp group.",
            "whatsapp_group_url": "https://chat.whatsapp.com/x", "whatsapp_group_note": "Joining the group also adds you."}""")!!
        assertTrue(status.confirmed)
        assertEquals("https://chat.whatsapp.com/x", status.whatsappUrl)
        assertEquals("$54", formDollars(status.total))
        assertEquals("$25.50", formDollars("25.5"))
    }
}
