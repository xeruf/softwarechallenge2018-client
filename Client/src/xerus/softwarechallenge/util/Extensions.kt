package xerus.softwarechallenge.util

import sc.plugin2018.Action
import sc.plugin2018.Field
import sc.plugin2018.FieldType
import sc.plugin2018.Move

fun Field.isType(type: FieldType) = this.type == type

fun Collection<Move>.str(): String = joinToString(separator = "\n", transform = { "| " + it.str() })

fun Move.str(): String {
    val out = StringBuilder("Move: ")
    for (action in actions)
        out.append(action.str()).append(", ")
    return out.substring(0, out.length - 2)
}

fun Action.str(): String {
    val str = toString()
    return str.substring(0, str.indexOf("order") - 1)
}