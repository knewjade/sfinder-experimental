package webpage

import kotlin.collections.List

class SourceBuilder(val indent: String, val separator: String) {
    private val builder = StringBuilder()
    private var count: Int = 0

    fun up() {
        count += 1
    }

    fun append(text: String) {
        with(builder) {
            append(indent * count)
            append(text)
            append(separator)
        }
    }

    fun down() {
        count -= 1
    }

    fun render(): String {
        return builder.toString()
    }

    private operator fun String.times(count: Int): String {
        return this.repeat(count)
    }
}

class Attribute(pairs: List<Pair<String, String>>) {
    constructor(vararg pairs: Pair<String, String>) : this(pairs.toList())

    constructor(pairs: Map<String, String>) : this(pairs.entries.map { it.key to it.value }.toList())

    val source: String = pairs.takeIf { it.isNotEmpty() }?.let { entries ->
        " " + entries.joinToString(" ") { "${it.first}=\"${it.second}\"" }
    } ?: ""
}

interface Element {
    fun render(builder: SourceBuilder)
}

abstract class Elements : Element {
    protected val children = mutableListOf<Element>()

    protected fun <T : Element> init(tag: T, define: T.() -> Unit = {}): T {
        tag.define()
        children.add(tag)
        return tag
    }

    fun custom(name: String, attr: Map<String, String> = mapOf(), define: Custom.() -> Unit) = init(Custom(name, attr), define)
}

// <tag key="value" ... > ... </tag>
abstract class Tag(val name: String, val attr: Attribute) : Elements() {
    constructor(name: String) : this(name, Attribute())

    constructor(name: String, attr: Map<String, String> = mapOf()) : this(name, Attribute(attr))

    override fun render(builder: SourceBuilder) {
        builder.append("<$name${attr.source}>")
        builder.up()
        children.forEach { it.render(builder) }
        builder.down()
        builder.append("</$name>")
    }
}

// <tag key="value" ... > ... </tag>
abstract class TagWithText(name: String, attr: Attribute) : Tag(name, attr) {
    constructor(name: String) : this(name, Attribute())

    constructor(name: String, attr: Map<String, String> = mapOf()) : this(name, Attribute(attr))

    fun text(value: String) {
        children.add(Text(value))
    }

    operator fun String.unaryPlus() {
        text(this)
    }
}

abstract class Def : Elements() {
    override fun render(builder: SourceBuilder) {
        children.forEach { it.render(builder) }
    }
}

class Text(val value: String) : Element {
    override fun render(builder: SourceBuilder) {
        builder.append(value)
    }
}

// <tag key="value" ... >
class FlatTag(val name: String, val attr: Attribute = Attribute()) : Element {
    override fun render(builder: SourceBuilder) {
        builder.append("<$name${attr.source}>")
    }
}

class Custom(name: String, attr: Map<String, String> = mapOf()) : TagWithText(name, attr)