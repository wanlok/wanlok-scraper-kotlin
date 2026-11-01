package com.wanlok.scraper.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageUrlsTest {
    @Test
    fun capLinks_giveAllEighteenPages() {
        val base = "https://www.cap.com.hk/tc/products/category/graphics-card"
        // The links in the pagination bar of page 1: the active page is "#".
        val links = listOf("$base#", "$base?page=2", "$base?page=3", "$base?page=2", "$base?page=18")

        val pages = PageUrls.build(base, "page", links)!!

        assertEquals(18, pages.size)
        assertEquals(base, pages.first())
        assertEquals("$base?page=2", pages[1])
        assertEquals("$base?page=18", pages.last())
    }

    @Test
    fun pegasusLinks_ignoreTheDisabledPageZero() {
        val base = "https://shop.pegasus.hk/categories/GPU"
        val links = listOf("$base?page=0", base, "$base?page=2", "$base?page=29", "$base?page=2")

        val pages = PageUrls.build(base, "page", links)!!

        assertEquals(29, pages.size)
        assertEquals(base, pages.first())
        assertEquals("$base?page=2", pages[1])
        assertEquals("$base?page=29", pages.last())
    }

    @Test
    fun otherParametersAreKept() {
        val start = "https://shop.example.com/list?sort=price"
        val links = listOf(
            "https://shop.example.com/list?sort=price&page=2&lang=tc",
            "https://shop.example.com/list?sort=price&page=5&lang=tc"
        )

        val pages = PageUrls.build(start, "page", links)!!

        assertEquals(
            listOf(
                start,
                "https://shop.example.com/list?sort=price&page=2&lang=tc",
                "https://shop.example.com/list?sort=price&page=3&lang=tc",
                "https://shop.example.com/list?sort=price&page=4&lang=tc",
                "https://shop.example.com/list?sort=price&page=5&lang=tc"
            ),
            pages
        )
    }

    @Test
    fun theParameterNameMustMatchExactly() {
        val start = "https://example.com/list"
        val links = listOf("https://example.com/list?page=9", "https://example.com/list?sp=4")

        assertNull(PageUrls.build(start, "p", links))
        assertEquals(
            listOf(start, "https://example.com/list?sort=1&p=2", "https://example.com/list?sort=1&p=3"),
            PageUrls.build(start, "p", listOf("https://example.com/list?sort=1&p=3"))
        )
    }

    @Test
    fun linksWithoutTheParameterGiveNull() {
        val start = "https://example.com/list"
        val links = listOf("https://example.com/detail/73J108LA3", "https://example.com/list#")

        assertNull(PageUrls.build(start, "page", links))
        assertNull(PageUrls.build(start, "page", emptyList()))
    }

    @Test
    fun onlyPageOne_givesJustTheStartUrl() {
        val start = "https://example.com/list"

        assertEquals(listOf(start), PageUrls.build(start, "page", listOf("$start?page=1")))
    }
}