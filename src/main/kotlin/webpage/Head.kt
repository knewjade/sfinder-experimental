package webpage


class Head : Tag("head") {
    fun title(define: Title.() -> Unit) = init(Title(), define)

    fun meta(define: Meta.() -> Unit) = init(Meta(), define)

    fun link(define: Link.() -> Unit) = init(Link(), define)
}

class Title : TagWithText("title")

class Meta : Def() {
    fun charset(charset: String) = init(FlatTag("meta", Attribute("charset" to charset)))

    fun viewport() = init(FlatTag("meta", Attribute("name" to "viewport", "content" to "width=device-width,initial-scale=1.0,minimum-scale=1.0")))
}

class Link : Def() {
    fun css(href: String) = init(FlatTag("link", Attribute("rel" to "stylesheet", "type" to "text/css", "href" to href)))
}
