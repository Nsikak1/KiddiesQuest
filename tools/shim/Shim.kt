package org.junit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Test

object Assert {
    @JvmStatic fun assertEquals(a: Any?, b: Any?) { if (a != b) throw AssertionError("expected <$a> but was <$b>") }
    @JvmStatic fun assertTrue(c: Boolean) { if (!c) throw AssertionError("expected true") }
    @JvmStatic fun assertTrue(m: String, c: Boolean) { if (!c) throw AssertionError(m) }
    @JvmStatic fun assertFalse(c: Boolean) { if (c) throw AssertionError("expected false") }
}
