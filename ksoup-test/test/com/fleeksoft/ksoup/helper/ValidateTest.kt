package com.fleeksoft.ksoup.helper

import com.fleeksoft.ksoup.exception.ValidationException
import com.fleeksoft.ksoup.helper.Validate.expectNotNull
import kotlin.test.*


class ValidateTest {
    @Test
    fun testWtf() {
        var threw = false
        try {
            Validate.wtf("Unexpected state reached")
        } catch (e: IllegalStateException) {
            threw = true
            assertEquals("Unexpected state reached", e.message)
        }
        assertTrue(threw)
    }

    @Test
    fun expectNotNull() {
        val foo = "Foo"
        val foo2 = expectNotNull<String?>(foo)
        assertSame(foo, foo2)

        // Test with a null object
        val bar: String? = null
        var threw = false
        try {
            expectNotNull<String?>(bar)
        } catch (e: ValidationException) {
            threw = true
            assertEquals("Object must not be null", e.message)
        }
        assertTrue(threw)
    }

    @Test
    fun testNotEmpty() {
        // Test with a non-empty string
        val str = "foo"
        Validate.notEmpty(str)

        // Test with an empty string
        var threw = false
        try {
            Validate.notEmpty("")
        } catch (e: ValidationException) {
            threw = true
            assertEquals("String must not be empty", e.message)
        }
        assertTrue(threw)

        // Test with a null string
        threw = false
        try {
            Validate.notEmpty(null)
        } catch (e: ValidationException) {
            threw = true
            assertEquals("String must not be empty", e.message)
        }
        assertTrue(threw)
    }

    @Test
    fun testIsTrue() {
        // Test with a true value
        Validate.isTrue(true)

        // Test with a false value
        var threw = false
        try {
            Validate.isTrue(false)
        } catch (e: ValidationException) {
            threw = true
            assertEquals("Must be true", e.message)
        }
        assertTrue(threw)
    }

    @Test
    fun testIsFalse() {
        // Test with a false value
        Validate.isFalse(false)

        // Test with a true value
        var threw = false
        try {
            Validate.isFalse(true)
        } catch (e: ValidationException) {
            threw = true
            assertEquals("Must be false", e.message)
        }
        assertTrue(threw)
    }

    @Test
    fun testAssertFail() {
        var result = false
        var threw = false
        try {
            result = Validate.assertFail("This should fail")
        } catch (e: ValidationException) {
            threw = true
            assertEquals("This should fail", e.message)
        }
        assertTrue(threw)
        assertFalse(result)
    }

    @Test
    fun testNotEmptyParam() {
        // Test with a non-empty string
        Validate.notEmptyParam("foo", "param")

        // Test with an empty string
        var threw = false
        try {
            Validate.notEmptyParam("", "param")
        } catch (e: ValidationException) {
            threw = true
            assertEquals("The 'param' parameter must not be empty.", e.message)
        }
        assertTrue(threw)

        // Test with a null string
        threw = false
        try {
            Validate.notEmptyParam(null, "param")
        } catch (e: ValidationException) {
            threw = true
            assertEquals("The 'param' parameter must not be empty.", e.message)
        }
        assertTrue(threw)
    }

    @Test
    fun testNotEmptyWithMessage() {
        // Test with a non-empty string
        Validate.notEmpty("foo", "Custom error message")

        // Test with an empty string
        var threw = false
        try {
            Validate.notEmpty("", "Custom error message")
        } catch (e: ValidationException) {
            threw = true
            assertEquals("Custom error message", e.message)
        }
        assertTrue(threw)

        // Test with a null string
        threw = false
        try {
            Validate.notEmpty(null, "Custom error message")
        } catch (e: ValidationException) {
            threw = true
            assertEquals("Custom error message", e.message)
        }
        assertTrue(threw)
    }
}