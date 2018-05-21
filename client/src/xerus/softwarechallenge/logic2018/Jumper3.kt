package xerus.softwarechallenge.logic2018

import sc.plugin2018.GameState
import sc.plugin2018.Move

object Jumper3 : CommonLogic("3.0.0") {
	
	override fun findBestMove(): Move? {
		return null
	}
	
	override fun evaluate(state: GameState) = Jumper1_8.evaluate(state)
	
	override fun defaultParams() = Jumper1_8.defaultParams()
	
}

class State : GameState() {

}