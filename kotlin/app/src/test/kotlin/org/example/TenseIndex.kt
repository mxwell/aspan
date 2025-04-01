package org.example

enum class TenseIndex(val index: Int) {
    presentTransitive(0),
    presentContinuousJatu(1),
    presentContinuousJuru(2),
    presentContinuousOtyru(3),
    presentContinuousTuru(4),
    presentContinuousJatuNegateAux(5),
    presentContinuousJuruNegateAux(6),
    presentContinuousOtyruNegateAux(7),
    presentContinuousTuruNegateAux(8),
    pastSimple(9),
    remotePast(10),
    remotePastNegateAux(11),
    conditionalMood(12),
    imperativeMood(13),
    optativeMood(14),
    ;

    fun isNegateAux(): Boolean {
        return when(this) {
            presentContinuousJatuNegateAux, presentContinuousJuruNegateAux, presentContinuousOtyruNegateAux, presentContinuousTuruNegateAux -> true
            remotePastNegateAux -> true
            else -> false
        }
    }

    fun isPresentContinuous(): Boolean {
        return when(this) {
            presentContinuousJatu, presentContinuousJuru, presentContinuousOtyru, presentContinuousTuru -> true
            presentContinuousJatuNegateAux, presentContinuousJuruNegateAux, presentContinuousOtyruNegateAux, presentContinuousTuruNegateAux -> true
            else -> false
        }
    }
}