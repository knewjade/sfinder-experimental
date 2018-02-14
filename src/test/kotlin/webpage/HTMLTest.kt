package webpage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class HTMLTest {
    fun HTML.flat(): String {
        return this.generate(indent = "", separator = "")
    }

    @Test
    fun title() {
        val result = html {
            head {
                title {
                    +"test"
                }
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><head><title>test</title></head></html>")
    }

    @Test
    fun meta() {
        val result = html {
            head {
                meta {
                    charset("UTF-8")
                    viewport()
                }
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0,minimum-scale=1.0\"></head></html>")
    }

    @Test
    fun link() {
        val result = html {
            head {
                link {
                    css("main.css")
                }
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"main.css\"></head></html>")
    }

    @Test
    fun body() {
        val result = html {
            head {}
            body(attr = mapOf("onload" to "init()")) {}
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><head></head><body onload=\"init()\"></body></html>")
    }

    @Test
    fun bodyWithText() {
        val result = html {
            head {}
            body {
                +"hoge"
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><head></head><body>hoge</body></html>")
    }

    @Test
    fun section() {
        val result = html {
            head {}
            body {
                section { +"hello world" }
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><head></head><body><section>hello world</section></body></html>")
    }

    @Test
    fun nav() {
        val result = html {
            head {}
            body {
                nav { +"hello world" }
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><head></head><body><nav>hello world</nav></body></html>")
    }

    @Test
    fun customInHTML() {
        val result = html {
            custom("first", attr = mapOf("id" to "hello")) {
                custom("second") {
                    +"foo"
                }
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><first id=\"hello\"><second>foo</second></first></html>")
    }

    @Test
    fun customInHead() {
        val result = html {
            head {
                custom("title") {
                    +"hello"
                }
            }
            body {
                custom("div") {
                    custom("span") {
                        +"hello"
                    }
                }
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><head><title>hello</title></head><body><div><span>hello</span></div></body></html>")
    }

    @Test
    fun article() {
        val result = html {
            body {
                article {
                    span(attr = mapOf("number" to "1")) { +"hello world" }
                }
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><body><article><span number=\"1\">hello world</span></article></body></html>")
    }

    @Test
    fun img() {
        val result = html {
            body {
                img(src = "main.png")
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><body><img src=\"main.png\"></body></html>")
    }

    @Test
    fun a() {
        val result = html {
            body {
                a(href = "main.html") {
                    span { +"hoge" }
                }
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><body><a href=\"main.html\"><span>hoge</span></a></body></html>")
    }

    @Test
    fun articleAndSection() {
        val result = html {
            body {
                article {
                    section {
                        +"hello"
                    }
                }
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><body><article><section>hello</section></article></body></html>")
    }

    @Test
    fun hr() {
        val result = html {
            body {
                hr()
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><body><hr></body></html>")
    }

    @Test
    fun ul() {
        val result = html {
            body {
                ul {
                    text("hoge")
                    a(href = "#test") { +"ok"}
                }
            }
        }

        assertThat(result.flat()).isEqualTo("<!doctype html><html><body><ul><li>hoge</li><li><a href=\"#test\">ok</a></li></ul></body></html>")
    }
}