package xerus.softwarechallenge.util

import sc.plugin2018.Move
import xerus.ktutil.helpers.Rater

/** speichert einen Zug und die dazugehörige Punktzahl  */
class MP: Rater<Move> {

    /** erzeugt einen leeren MP  */
    constructor(): super()

    /** erzeugt einen neuen MP mit dem gegebenen Zug und den Punkten  */
    constructor(m: Move, p: Double): super(m, p)

    override fun toString(): String {
        return "%s Punkte: %d".format(LogicHandler.toString(obj!!), points)
    }

}
