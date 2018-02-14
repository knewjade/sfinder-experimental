package webpage

fun html(func: HTML.() -> Unit): HTML {
    val html = HTML()
    html.func()
    return html
}

class HTML : Tag("html") {
    override fun render(builder: SourceBuilder) {
        builder.append("<!doctype html>")
        super.render(builder)
    }

    fun head(define: Head.() -> Unit) = init(Head(), define)

    fun body(attr: Map<String, String> = mapOf(), define: Body.() -> Unit) = init(Body(attr), define)

    fun generate(indent: String = "  ", separator: String = System.lineSeparator()): String {
        val builder = SourceBuilder(indent, separator)
        render(builder)
        return builder.render()
    }
}