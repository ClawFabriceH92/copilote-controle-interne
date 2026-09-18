package fr.fh.copiloteci

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** État d'une séance : questionnaire, réponses, transcription, chrono. */
class SeanceViewModel : ViewModel() {

    var trame by mutableStateOf<Trame?>(null)
        private set
    var dossier by mutableStateOf("")
        private set
    var participants by mutableStateOf("")
        private set

    val etats = mutableStateMapOf<String, Etat>()
    val segments = mutableStateListOf<Segment>()
    val detections = mutableStateListOf<Apparieur.Resultat>()
    val actions = mutableStateListOf<ActionSuivre>()

    var secondes by mutableIntStateOf(0)
        private set
    var enregistrement by mutableStateOf(false)
        private set
    var transcriptionActive by mutableStateOf(false)
        private set

    fun demarrer(nouvelle: Trame, dossierClient: String, participantsSaisis: String) {
        trame = nouvelle
        dossier = dossierClient
        participants = participantsSaisis
        etats.clear()
        nouvelle.questions.forEach { etats[it.id] = Etat() }
        segments.clear()
        detections.clear()
        actions.clear()
        secondes = 0
        enregistrement = true
    }

    fun basculerEnregistrement() {
        enregistrement = !enregistrement
    }

    fun activerTranscription(active: Boolean) {
        transcriptionActive = active
    }

    fun tic() {
        if (enregistrement) secondes += 1
    }

    fun horodatage(): String {
        val h = secondes / 3600
        val m = (secondes % 3600) / 60
        val s = secondes % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    /** Enregistre un segment de conversation et met à jour les questions évoquées. */
    fun ajouterSegment(intervenant: String, texte: String, final: Boolean) {
        val propre = texte.trim()
        if (propre.length < 3) return
        segments += Segment(intervenant.ifBlank { "Intervenant" }, propre, horodatage())
        val questions = trame?.questions ?: return
        val aEvaluer = questions.filter {
            val statut = etats[it.id]?.statut ?: EtatQuestion.A_POSER
            statut == EtatQuestion.A_POSER || statut == EtatQuestion.EVOQUEE
        }
        val resultats = Apparieur.evaluer(propre, aEvaluer)
        detections.clear()
        resultats.forEach { r ->
            detections += r
            val etat = etats[r.questionId] ?: Etat()
            if (etat.statut != EtatQuestion.REPONDUE) {
                etats[r.questionId] = etat.copy(
                    statut = EtatQuestion.EVOQUEE,
                    citation = Citation(r.extrait, horodatage(), r.confiance, "auto")
                )
            }
        }
        if (final) detections.clear()
    }

    fun marquer(id: String, statut: EtatQuestion) {
        val etat = etats[id] ?: Etat()
        val citation = etat.citation ?: Citation(
            extrait = "coché par le collaborateur",
            horodatage = horodatage(),
            confiance = 100,
            origine = "manuel"
        )
        etats[id] = if (statut == EtatQuestion.A_POSER) Etat()
        else etat.copy(statut = statut, citation = citation)
    }

    fun noter(id: String, texte: String) {
        val etat = etats[id] ?: Etat()
        etats[id] = etat.copy(note = texte)
    }

    fun ajouterAction(intitule: String, questionId: String) {
        if (intitule.isBlank()) return
        actions += ActionSuivre(intitule.trim(), questionId)
    }

    fun questionSuggeree(): Question? {
        val questions = trame?.questions ?: return null
        return questions.firstOrNull { etats[it.id]?.statut == EtatQuestion.EVOQUEE }
            ?: questions.firstOrNull { etats[it.id]?.statut == EtatQuestion.A_POSER }
    }

    fun traitees(): Int = etats.count { it.value.statut == EtatQuestion.REPONDUE || it.value.statut == EtatQuestion.SANS_OBJET }

    fun total(): Int = trame?.nbQuestions ?: 0

    fun resteAPoser(): List<Question> =
        (trame?.questions ?: emptyList()).filter { etats[it.id]?.statut == EtatQuestion.A_POSER }

    fun ecarts(): List<Question> =
        (trame?.questions ?: emptyList()).filter { etats[it.id]?.statut == EtatQuestion.EVOQUEE }

    fun progression(): Float {
        val total = total()
        return if (total == 0) 0f else traitees().toFloat() / total.toFloat()
    }

    fun reinitialiser() {
        trame = null
        dossier = ""
        participants = ""
        etats.clear()
        segments.clear()
        detections.clear()
        actions.clear()
        secondes = 0
        enregistrement = false
        transcriptionActive = false
    }
}
