import java.lang.reflect.InvocationTargetException
fun main() {
    val c = Class.forName("ng.name.nsikak.kidquest.CoreLogicTest")
    var pass = 0; var fail = 0
    for (m in c.declaredMethods.sortedBy { it.name }) {
        if (m.getAnnotation(org.junit.Test::class.java) == null) continue
        try { m.invoke(c.getDeclaredConstructor().newInstance()); pass++; println("PASS ${m.name}") }
        catch (e: InvocationTargetException) { fail++; println("FAIL ${m.name}: ${e.cause}") }
    }
    println("\n$pass passed, $fail failed")
    System.exit(if (fail == 0) 0 else 1)
}
