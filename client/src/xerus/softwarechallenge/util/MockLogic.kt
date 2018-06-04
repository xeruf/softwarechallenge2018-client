package xerus.softwarechallenge.util

import sc.plugin2018.*
import sc.shared.GameResult
import sc.shared.PlayerColor

object MockLogic : IGameHandler {
	override fun onRequestAction() {
		System.exit(0)
	}
	
	override fun gameEnded(p0: GameResult?, p1: PlayerColor?, p2: String?) {
	}
	
	override fun onUpdate(p0: Player?, p1: Player?) {
	}
	
	override fun onUpdate(p0: GameState?) {
	}
	
	override fun sendAction(p0: Move?) {
	}
}