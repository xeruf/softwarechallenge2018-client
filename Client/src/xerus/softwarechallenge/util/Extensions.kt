package xerus.softwarechallenge.util

import sc.plugin2018.*

inline fun Field.isType(type: FieldType) = this.type == type

fun Move.str(): String {
	val out = StringBuilder("Move: ")
	for (action in actions)
		out.append(action.str()).append(", ")
	return out.substring(0, out.length - 2)
}

fun Move.add(action: Action) = also { it.actions.add(action) }

fun Collection<Move>.str(): String = joinToString(prefix = "| ", separator = "\n| ", transform = { it.str() })

inline fun MutableCollection<Move>.addMove(vararg actions: Action) = add(Move(*actions))

fun Action.str(): String {
	val str = toString()
	return str.substring(0, str.indexOf("order") - 1)
}