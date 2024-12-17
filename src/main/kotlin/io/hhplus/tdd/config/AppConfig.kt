package io.hhplus.tdd.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser

@Configuration
class AppConfig {

    @Bean
    fun expressionParser(): ExpressionParser {
        return SpelExpressionParser()
    }
}