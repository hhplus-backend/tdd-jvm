package io.hhplus.tdd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication
@EnableAspectJAutoProxy
class TddApplication

fun main(args: Array<String>) {
    runApplication<TddApplication>(*args)
}
