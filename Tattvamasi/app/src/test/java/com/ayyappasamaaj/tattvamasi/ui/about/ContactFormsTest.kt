package com.ayyappasamaaj.tattvamasi.ui.about

import com.ayyappasamaaj.tattvamasi.ui.guides.aboutGuide
import com.ayyappasamaaj.tattvamasi.ui.guides.parseGuides
import com.ayyappasamaaj.tattvamasi.ui.socialApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** About > Contact's forms (website: /api/pages/contact/, /api/pages/mailing-list/), its rows, and its social links. */
class ContactFormsTest {
    @Test
    fun readsWriteToUsTopicsAndPrefill() {
        val form = parseContactForm(
            """{"topics": [{"value": "puja", "label": "Sponsoring a puja"}, {"value": "volunteer", "label": "Volunteering"},
               {"value": "other", "label": "Something else"}, {"value": "", "label": "Nothing"}],
               "topic": "other", "name": "Murali N", "email": "m@example.com"}""",
        )!!
        assertEquals(listOf("puja", "volunteer", "other"), form.topics.map { it.value })
        assertEquals("Sponsoring a puja", form.topics[0].label)
        assertEquals("other", form.topic)
        assertEquals("Murali N", form.name)
        assertEquals("m@example.com", form.email)
        // Signed out: nothing filled in; a default the topics don't have becomes the first
        val signedOut = parseContactForm("""{"topics": [{"value": "a", "label": "A"}], "topic": "zz", "name": "", "email": ""}""")!!
        assertEquals("a", signedOut.topic)
        assertEquals("", signedOut.name)
        // Unusable: no topics, or not JSON
        assertNull(parseContactForm("""{"topics": [], "topic": "other"}"""))
        assertNull(parseContactForm("<html>Server Error</html>"))
    }

    @Test
    fun readsTheWebsitesAnswers() {
        assertEquals(FormReply.Done("Thank you. We'll reply by email."),
            parseFormReply(200, """{"message": "Thank you. We'll reply by email."}"""))
        assertEquals(FormReply.Done("Please confirm from the email we sent.", "sent"),
            parseFormReply(200, """{"status": "sent", "message": "Please confirm from the email we sent."}"""))
        assertEquals(FormReply.Done("You're subscribed.", "subscribed"),
            parseFormReply(200, """{"status": "subscribed", "message": "You're subscribed."}"""))
        // The website's errors by field, shown under each; a list of them is joined; the whole form's go under it
        assertEquals(
            FormReply.Problems(mapOf("email" to "Enter a valid email address.", "message" to "This field is required.",
                "" to "Something else.")),
            parseFormReply(400, """{"errors": {"email": "Enter a valid email address.",
                "message": ["This field is required."], "__all__": "Something else."}}"""),
        )
        // Too many messages: the website's reason under the form
        assertEquals(FormReply.Problems(mapOf("" to "Too many messages. Please try again later.")),
            parseFormReply(429, """{"error": "Too many messages. Please try again later."}"""))
        assertEquals(FormReply.Problems(mapOf("" to NO_CONNECTION)), parseFormReply(0, ""))
        val other = parseFormReply(500, "<html>error</html>") as FormReply.Problems
        assertTrue("info@ayyappasamaaj.org" in other.errors.getValue(""))
        // A 200 without its sentence isn't taken as sent
        assertTrue(parseFormReply(200, "{}") is FormReply.Problems)
    }

    @Test
    fun contactKeepsItsRowsAndOrder() {
        assertEquals(
            listOf("Write to us", "Join the mailing list", "Facebook", "YouTube", "Instagram", "X", "Community partners",
                "Privacy policy", "Terms and conditions", "Cookie policy", "Accessibility", "Credits"),
            contactLinks.map { it.title },
        )
        assertEquals(ContactAction.Form(ContactPage.WriteToUs), contactLinks[0].action)
        assertEquals(ContactAction.Form(ContactPage.MailingList), contactLinks[1].action)
        assertTrue(contactLinks.subList(2, 6).all { it.action == ContactAction.Social })
        // The pages' slugs are the guides feed's "about_pages" (the shared contract, section C)
        assertEquals(
            listOf("community-partners", "privacy-policy", "terms", "cookie-policy", "accessibility", "credits"),
            contactLinks.mapNotNull { (it.action as? ContactAction.Page)?.slug },
        )
    }

    @Test
    fun aPageReadsInTheAppWhenTheFeedHasIt() {
        val sample = javaClass.classLoader!!.getResource("guides-about-sample.json")!!.readText()
        val guides = parseGuides(sample)!!
        assertEquals("Privacy policy", aboutGuide("privacy-policy", guides)!!.name)
        // An About page that's all introduction is kept; one with nothing in it isn't (the browser instead)
        assertEquals("credits", aboutGuide("credits", guides)!!.slug)
        assertNull(aboutGuide("cookie-policy", guides))
        // Not in the feed (an older website): the browser
        assertNull(aboutGuide("terms", guides))
        // Only "about" pages: a festival's slug isn't one
        assertNull(aboutGuide("vishu", guides))
        // An older copy without about pages
        val older = javaClass.classLoader!!.getResource("guides-sample.json")!!.readText()
        assertNull(aboutGuide("privacy-policy", parseGuides(older)!!))
    }

    @Test
    fun socialLinksOpenTheirAppOnlyWhenOneIsInstalled() {
        val browsers = setOf("com.android.chrome", "org.mozilla.firefox")
        // Only browsers take the link: the in-app browser
        assertNull(socialApp(listOf("com.android.chrome"), browsers))
        assertNull(socialApp(emptyList(), browsers))
        // The network's app takes it
        assertEquals("com.facebook.katana", socialApp(listOf("com.android.chrome", "com.facebook.katana"), browsers))
        assertEquals("com.google.android.youtube", socialApp(listOf("com.google.android.youtube"), browsers))
    }
}
