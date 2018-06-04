import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.annotations.XStreamAlias
import sc.plugin2018.FallBack
import sc.plugin2018.Field
import sc.plugin2018.FieldType
import xerus.ktutil.toInt
import java.io.File

val file = File("/home/janek/Daten/Downloads/debug")
val out = File("/home/janek/Daten/Downloads/turn.xml")

fun main(args: Array<String>) {
	val lines = file.readLines()
	val split = lines[0].split(" Track[")
	val turn = split[0].split(" ").last().toInt()
	val track = split[1].dropLast(1).split(", ").map { it.split(" ") }.map {
		Field(FieldType.valueOf(it[1]), it[0].toInt())
	}
	
	val players = lines.drop(1).map { it.split(" ").drop(3) }
	
	out.writeText("""<protocol>
    <state currentPlayer="${players[0][0]}" class="state" startPlayer="RED" turn="$turn">""")
	println(players)
	val ind = (players[0][0] == "BLUE").toInt()
	println(ind)
	for (i in arrayOf(ind, (ind + 1) % 2)) {
		val player = players[i]
		out.appendText("""
      <${player[0].toLowerCase()} carrots="${player[3].drop(1).toInt()}" displayName="Test" salads="${player[4].takeLast(1).toInt()}" index="${player[2].takeWhile { it.isNumber() }.toInt()}" color="${player[0]}">
        <cards>${player.subList(5, player.size - 2).map { it.trim('[', ']', ',') }.filterNot { it.isBlank() }.joinToString("") { "\n          <type>$it</type>" } }
        </cards>
		<lastNonSkipAction order="0" class="${toAction(player.last()).first}" ${toAction(player.last()).second?.let { "distance=\"$it\"" } ?: ""}/>
      </${player[0].toLowerCase()}>""")
	}
	out.appendText("""
      <board>
        ${track.joinToString("\n        ") { "<fields type=\"${it.type}\" index=\"${it.index}\"/>" }}
        <fields type="START" index="0"/>
      </board>
      <lastMove>
        <${toAction(players[1].last()).first} order="0" ${toAction(players[1].last()).second?.let { "distance=\"$it\"" } ?: ""}/>
      </lastMove>""")
	out.appendText("\n    </state>\n</protocol>")
}

fun toAction(string: String): Pair<String, Int?> {
	val other = string.dropLastWhile { it.isNumber() }
	val annotation = Class.forName("sc.plugin2018.$other").annotations.find { it is XStreamAlias } as XStreamAlias
	val int = string.substring(other.length).toIntOrNull()
	return annotation.value to int
}

fun Char.isNumber() = Regex("[0-9]").matches(toString())