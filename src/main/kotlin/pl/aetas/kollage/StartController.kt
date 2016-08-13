package pl.aetas.kollage

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class StartController {
    @RequestMapping("/start")
    fun greeting(): String {
        return "It's just a start";
    }
}


//@SpringBootApplication
//open class Application {
//    companion object {
//        @JvmStatic public fun main(args: Array<String>) {
//            SpringApplication.run(Application::class.java, *args)
//        }
//    }
//}