import javafx.collections.FXCollections
import javafx.scene.Scene
import javafx.scene.chart.*
import xerus.ktutil.javafx.applySkin
import xerus.ktutil.javafx.ui.App
import java.io.File

val run = Runtime.getRuntime()

fun main(args: Array<String>) {
	App.launch {
		val xAxis = CategoryAxis()
		val yAxis = NumberAxis(0.2, 0.8, 0.1)
		yAxis.label = "Winrate"
		val chart = LineChart(xAxis, yAxis)
		val hashes = run.exec("git log --oneline -10").inputStream.bufferedReader().readLines().map { it.substringBefore(' ') }.asReversed()
		xAxis.isAutoRanging = false
		xAxis.categories.addAll(hashes)
		for (i in 6..8) {
			val values = XYChart.Series<String, Number>()
			values.name = "Jumper1_$i"
			val files = File("../tests$i").listFiles().sorted()
			files.forEach { file ->
				val lines = file.readLines().dropWhile { !it.contains("SCORES") }
				val new = lines[1].split("-", ": ")
				val old = lines[2].split("-", ": ")
				val newScore = new[2].toDouble()
				val oldScore = old[2].toDouble()
				if (new[1] !in hashes)
					println("Invalid hash ${new[1]} in $file")
				val winrate = newScore / (newScore + oldScore)
				values.data.add(XYChart.Data(new[1], winrate))
			}
			chart.data.add(values)
			/*val trend = XYChart.Series<String, Number>()
			trend.name = "Jumper1_$i Trend"
			var value = 0.5
			trend.data.add(XYChart.Data(hashes[0], value))
			for (j in 1..hashes.lastIndex) {
				val hash = hashes[j]
				value *= (values.data.find { it.xValue == hash } ?: continue).yValue.toDouble() * 2
				trend.data.add(XYChart.Data<String, Number>(hash, value))
			}
			chart.data.add(trend)*/
		}
		chart.data.add(XYChart.Series<String, Number>("Average",
				FXCollections.observableArrayList(hashes.map { hash -> XYChart.Data<String, Number>(hash, chart.data.mapNotNull { it.data.find { it.xValue == hash }?.yValue?.toDouble() }.average()) })))
		chart.setPrefSize(hashes.size * 75.0, 500.0)
		Scene(chart).also { it.applySkin("beige") }
	}
}