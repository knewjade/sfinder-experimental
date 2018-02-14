package webpage

abstract class InlineElement(name: String, attr: Attribute) : TagWithText(name, attr) {
    constructor(name: String) : this(name, Attribute())

    fun span(attr: Map<String, String> = mapOf(), define: Span.() -> Unit) = init(Span(attr), define)

    fun a(href: String, attr: Map<String, String> = mapOf(), define: A.() -> Unit): A {
        val newAttr = mutableMapOf<String, String>()
        newAttr.putAll(attr)
        newAttr["href"] = href
        return init(A(newAttr), define)
    }
}

abstract class Paragraph(name: String, attr: Attribute = Attribute()) : InlineElement(name, attr) {
    constructor(name: String, attr: Map<String, String> = mapOf()) : this(name, Attribute(attr))

    fun nav(attr: Map<String, String> = mapOf(), define: Nav.() -> Unit) = init(Nav(attr), define)

    fun section(attr: Map<String, String> = mapOf(), define: Section.() -> Unit) = init(Section(attr), define)

    fun article(attr: Map<String, String> = mapOf(), define: Article.() -> Unit) = init(Article(attr), define)

    fun img(src: String, attr: Map<String, String> = mapOf()) {
        val newAttr = mutableMapOf<String, String>()
        newAttr.putAll(attr)
        newAttr["src"] = src
        init(FlatTag("img", Attribute(newAttr)))
    }

    fun ul(attr: Map<String, String> = mapOf(), define: List.() -> Unit) = init(List("ul", attr), define)

    fun hr() = init(FlatTag("hr"))
}

class Body(attr: Map<String, String>) : Paragraph("body", attr)

class Nav(attr: Map<String, String>) : Paragraph("nav", attr)

class Section(attr: Map<String, String>) : Paragraph("section", attr)

class Article(attr: Map<String, String>) : Paragraph("article", attr)

class Span(attr: Map<String, String>) : Paragraph("span", attr)

class A(attr: Map<String, String>) : Paragraph("a", attr)

class List(name: String, attr: Map<String, String>) : Tag(name, attr) {
    fun text(value: String) {
        val item = Item("li")
        item.text(value)
        init(item)
    }

    fun a(href: String, attr: Map<String, String> = mapOf(), define: A.() -> Unit) {
        val item = Item("li")
        item.a(href, attr, define)
        init(item)
    }
}

class Item(name: String) : InlineElement(name)
