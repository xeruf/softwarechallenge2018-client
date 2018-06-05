Jumper3:
 - Finde für den aktuellen GameState Züge und füge sie der Queue hinzu
 - Solange du Zeit hast und noch was in der Queue ist:
   - Nimm einen Zug aus der Queue
   - Suche bis in einer Tiefe von 2 die maximal 2 besten Züge für den Gegner, für jeden davon:
     - Finde Züge für dich selbst
     - Teste jeden dieser Züge und finde den Besten
     - Füge den GameState, der aus dem Test resultiert, der Queue hinzu, wenn die Bewertung einen Threshold überschreitet
     - Bilde den Durchschnitt der besten Züge, da du ja nicht weißt, welchen der Züge der Gegner ausführen wird
       (Ansonsten gab es oft das Problem, dass der Client auf einen bestimmten gegnerischen Zug spekuliert hat)
   - Wenn du alle Züge einer Ebene durchhast, finde heraus, welcher Zug nach den aktuellen Bewertungen der beste ist
 - Gib den zuletzt berechneten besten Zug zurück