package xerus.softwarechallenge.logic2018

import sc.plugin2018.GameState
import sc.plugin2018.Move

class Jumper3: LogicBase("3.0.0") {
	
	override fun findBestMove(): Move? {
		return null
	}
	
	override fun GameState.findMoves(): List<Move> {
		return listOf()
	}
	
	override fun evaluate(state: GameState): Double {
		return 0.0
	}
	
	override fun defaultParams(): DoubleArray {
		return doubleArrayOf()
	}
	
}