package org.neo4j.graphql

import demo.org.neo4j.graphql.TckTest
import graphql.language.Node
import graphql.language.VariableReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class CypherDirectiveTest {

    val schema = """
type Person {
    id: ID
    name: String @cypher(statement:"RETURN this.name")
    age(mult:Int=13) : [Int] @cypher(statement:"RETURN this.age * mult as age")
}
type Query {
    person : [Person]
    p2: [Person] @cypher(statement:"MATCH (p:Person) RETURN p")
    p3(name:String): Person @cypher(statement:"MATCH (p:Person) WHERE p.name = name RETURN p LIMIT 1")
}
"""

    @Test
    fun renderCypherFieldDirective() {

        val expected = """MATCH (person:Person) RETURN person { name:apoc.cypher.runFirstColumnSingle('WITH ${"$"}this AS this RETURN this.name',{this:person}) } AS person"""
        val query = """{ person { name }}"""
        assertQuery(query, expected, emptyMap())
    }

    @Test
    fun renderCypherFieldDirectiveWithParamsDefaults() {

        val expected = """MATCH (person:Person) RETURN person { age:apoc.cypher.runFirstColumnMany('WITH ${"$"}this AS this,${'$'}mult AS mult RETURN this.age * mult as age',{this:person,mult:${'$'}personMult}) } AS person"""
        val query = """{ person { age }}"""
        assertQuery(query, expected, mapOf("personMult" to 13))
    }

    @Test
    fun renderCypherFieldDirectiveWithParams() {

        val expected = """MATCH (person:Person) RETURN person { age:apoc.cypher.runFirstColumnMany('WITH ${"$"}this AS this,${'$'}mult AS mult RETURN this.age * mult as age',{this:person,mult:${'$'}personMult}) } AS person"""
        val query = """{ person { age(mult:25) }}"""
        assertQuery(query, expected, mapOf("personMult" to 25L))
    }

    @Test
    fun renderCypherQueryDirective() {
        val expected = """UNWIND apoc.cypher.runFirstColumnMany('MATCH (p:Person) RETURN p',{}) AS p2 RETURN p2 { .id } AS p2"""
        val query = """{ p2 { id }}"""
        assertQuery(query, expected, emptyMap())
    }
    @Test
    fun renderCypherQueryDirectiveParams() {
        val expected = """UNWIND apoc.cypher.runFirstColumnSingle('WITH ${'$'}name AS name MATCH (p:Person) WHERE p.name = name RETURN p LIMIT 1',{name:${'$'}p3Name}) AS p3 RETURN p3 { .id } AS p3"""
        val query = """{ p3(name:"Jane") { id }}"""
        assertQuery(query, expected, mapOf("p3Name" to "Jane"))
    }
    @Test
    fun renderCypherQueryDirectiveParamsArgs() {
        val expected = """UNWIND apoc.cypher.runFirstColumnSingle('WITH ${'$'}name AS name MATCH (p:Person) WHERE p.name = name RETURN p LIMIT 1',{name:${'$'}pname}) AS p3 RETURN p3 { .id } AS p3"""
        val query = """query(${'$'}pname:String) { p3(name:${'$'}pname) { id }}"""
        assertQuery(query, expected, mapOf("pname" to VariableReference("pname")))
    }

    @Test @Ignore
    fun testTck() {
        TckTest(schema).testTck("cypher-directive-test.md", 0)
    }

    private fun assertQuery(query: String, expected: String, params : Map<String,Any?> = emptyMap()) {
        val result = Translator(SchemaBuilder.buildSchema(schema)).translate(query).first()
        assertEquals(expected, result.query)
        assertTrue("${params} IN ${result.params}", params.all { val v=result.params[it.key]; when (v) { is Node -> v.isEqualTo(it.value as Node) else -> v == it.value}})
    }
}