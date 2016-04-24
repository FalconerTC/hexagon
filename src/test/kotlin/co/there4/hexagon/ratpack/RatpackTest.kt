package co.there4.hexagon.ratpack

import org.testng.annotations.Test
import co.there4.hexagon.rest.HttpClient
import ratpack.registry.Registry
import ratpack.server.*
import java.net.URL

@Test class RatpackTest {
    fun test_app_from_handler() {
        appFromHandler {
            render("Hello World!")
        }
        .check {
            assert("Hello World!" == text)
        }
    }

    fun test_template_renderer() {
        appFromHandlers {
            get ("hello") {
                template("pebble_template.html", mapOf (
                    "title" to "Greeting",
                    "content" to "Hello World!"
                ))
            }
            get ("no_localized") {
                template ("no_localized.txt")
            }
        }
        .check {
            val body = getBody("hello")
            assert(body.contains ("<title>Greeting</title>"))
            assert(body.contains ("<p>globalValue</p>"))
            assert(body.contains ("<p>commonValue</p>"))
            assert(body.contains ("<p>Hello World!</p>"))
            assert(body.contains ("<p>english</p>"))
            assert(getBody("no_localized")?.trim () == "no localized")
        }
    }

    fun test_app_from_handlers() {
        appFromHandlers {
            get ("hello") {
                render("Hello World!")
            }
            get ("bye") {
                render("Good bye cruel World!")
            }
        }
        .check {
            assert("Hello World!" == getBody("hello"))
            assert("Good bye cruel World!" == getBody("bye"))
        }
    }

    fun test_app_from_server() {
        val server = serverOf {
            serverConfig {
                port(0)
            }
            handlers {
                get ("hello") {
                    render("Hello World!")
                }
                get ("bye") {
                    render("Good bye cruel World!")
                }
            }
        }

        appFromServer (server).check {
            assert("Hello World!" == getBody("hello"))
            assert("Good bye cruel World!" == getBody("bye"))
        }
    }

    fun test_server_start() {
        fun KContext.renderMethod() { render("${request.method} ${pathTokens["name"]}") }

        val server = serverStart {
            serverConfig {
                port(0)
                baseDir(BaseDir.find("logback-test.xml"))
            }
            registry { add("World!") }
            handlers {
                get { render("Hello " + get(String::class.java)) }

                get("get/:name") { renderMethod() }
                put("put/:name") { renderMethod() }
                post("post/:name") { renderMethod() }
                delete("delete/:name") { renderMethod() }
                options("options/:name") { renderMethod() }
                patch("patch/:name") { renderMethod() }

                path ("path/:name") {
                    byMethod {
                        get { renderMethod() }
                        put { renderMethod() }
                        post { renderMethod() }
                        delete { renderMethod() }
                        options { renderMethod() }
                        patch { renderMethod() }
                    }
                }

                fileSystem {
                    files { it.dir("public") }
                }
            }
        }

        val client = HttpClient (URL ("http://${server.bindHost}:${server.bindPort}/"))

        assert (client.getBody() == "Hello World!")

        assert (client.getBody("get/john") == "GET john")
        assert (client.putBody("put/john", "body") == "PUT john")
        assert (client.postBody("post/john", "body") == "POST john")
        assert (client.deleteBody("delete/john") == "DELETE john")
        assert (client.optionsBody("options/john") == "OPTIONS john")
        assert (client.patchBody("patch/john", "body") == "PATCH john")

        assert (client.getBody("path/john") == "GET john")
        assert (client.putBody("path/john", "body") == "PUT john")
        assert (client.postBody("path/john", "body") == "POST john")
        assert (client.deleteBody("path/john") == "DELETE john")
        assert (client.optionsBody("path/john") == "OPTIONS john")
        assert (client.patchBody("path/john", "body") == "PATCH john")

        assert (client.getBody("file.txt")?.trim() == "file content")

        server.stop()
    }

    fun test_prefix() {
        data class Person(val id: String, val status: String, val age: String)

        appFromHandlers {
            prefix("person/:id") {
                all {
                    val id: String = pathTokens["id"].toString()
                    val person = Person (id, "example-status", "example-age")
                    next(Registry.single(Person::class.java, person))
                }
                get("status") {
                    val person = get(Person::class.java)
                    render("person ${person.id} status: ${person.status}")
                }
                get("age") {
                    val person = get(Person::class.java)
                    render("person ${person.id} age: ${person.age}")
                }
            }
        }
        .check {
            assert("person 10 status: example-status" == getBody("person/10/status"))
            assert("person 6 age: example-age" == getBody("person/6/age"))
        }
    }
}