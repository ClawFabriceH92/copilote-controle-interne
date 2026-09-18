package fr.fh.copiloteci

/** Modèle de données du copilote de contrôle interne. */

enum class EtatQuestion(val libelle: String) {
    A_POSER("à poser"),
    EVOQUEE("évoquée"),
    REPONDUE("répondue"),
    SANS_OBJET("sans objet")
}

data class Question(
    val id: String,
    val question: String,
    val attendu: String,
    val relance: String,
    val motsCles: List<String>,
    val pieces: List<String>
)

data class Section(val titre: String, val questions: List<Question>)

data class Trame(
    val id: String,
    val nom: String,
    val version: String,
    val source: String,
    val dureeCibleMin: Int,
    val participantsAttendus: List<String>,
    val sections: List<Section>
) {
    val questions: List<Question> get() = sections.flatMap { it.questions }
    val nbQuestions: Int get() = questions.size
}

data class Citation(
    val extrait: String,
    val horodatage: String,
    val confiance: Int,
    val origine: String
)

data class Etat(
    val statut: EtatQuestion = EtatQuestion.A_POSER,
    val citation: Citation? = null,
    val note: String = ""
)

data class Segment(
    val intervenant: String,
    val texte: String,
    val horodatage: String
)

data class ActionSuivre(
    val intitule: String,
    val questionId: String
)
