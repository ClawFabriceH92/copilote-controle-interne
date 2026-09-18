package fr.fh.copiloteci

import java.text.Normalizer

/**
 * Appariement question ↔ conversation, niveau 1 : mots-clés.
 *
 * Le vocabulaire est normalisé (accents, casse, ponctuation) de sorte qu'un
 * segment parlé déclenche la détection même si la formulation diffère.
 * Règle : 1 mot-clé = indice faible, 2 mots-clés distincts = question « évoquée »
 * (l'état « répondue » reste une décision du collaborateur).
 */
object Apparieur {

    const val SEUIL_EVOQUEE = 2

    fun normaliser(texte: String): String {
        val sansAccent = Normalizer.normalize(texte, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return sansAccent.lowercase().replace(Regex("[^a-z0-9]"), "")
    }

    /** Mots-clés de la question retrouvés dans le segment, dédupliqués. */
    fun indices(segmentNormalise: String, question: Question): List<String> =
        question.motsCles.filter { it.length >= 3 && segmentNormalise.contains(it) }.distinct()

    /**
     * Score de recouvrement : nombre de mots-clés présents, pondéré par la longueur
     * du segment (un segment court très ciblé vaut mieux qu'un long monologue).
     */
    fun score(segment: String, question: Question): Int {
        val normalise = normaliser(segment)
        if (normalise.length < 8) return 0
        return indices(normalise, question).size
    }

    /** Confiance affichée (indicative), entre 0 et 99 %. */
    fun confiance(trouves: Int, nbMotsCles: Int, longueurSegment: Int): Int {
        if (nbMotsCles == 0) return 0
        val couverture = trouves.toDouble() / nbMotsCles.toDouble()
        val base = 40 + (couverture * 55).toInt()
        val bonus = if (longueurSegment in 30..400) 5 else 0
        return (base + bonus).coerceIn(0, 99)
    }

    data class Resultat(
        val questionId: String,
        val extrait: String,
        val trouves: List<String>,
        val score: Int,
        val confiance: Int
    )

    /** Questions atteignant le seuil pour un segment donné, la plus forte d'abord. */
    fun evaluer(segment: String, questions: List<Question>, limite: Int = 3): List<Resultat> {
        val normalise = normaliser(segment)
        if (normalise.length < 8) return emptyList()
        return questions
            .mapNotNull { q ->
                val trouves = indices(normalise, q)
                if (trouves.size < SEUIL_EVOQUEE) null
                else Resultat(
                    questionId = q.id,
                    extrait = segment.trim(),
                    trouves = trouves,
                    score = trouves.size,
                    confiance = confiance(trouves.size, q.motsCles.size, normalise.length)
                )
            }
            .sortedByDescending { it.score }
            .take(limite)
    }
}
