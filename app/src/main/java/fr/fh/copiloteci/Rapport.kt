package fr.fh.copiloteci

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Compte rendu de séance : texte markdown + PDF, écrits dans le stockage de l'application. */
object Rapport {

    private const val LARGEUR = 595
    private const val HAUTEUR = 842
    private const val MARGE = 42f
    private const val LIGNE = 13.5f

    fun texte(vm: SeanceViewModel): String {
        val trame = vm.trame ?: return ""
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date())
        val sb = StringBuilder()
        sb.appendLine("# Compte rendu de séance — contrôle interne")
        sb.appendLine()
        sb.appendLine("- Dossier : ${vm.dossier.ifBlank { "non renseigné" }}")
        sb.appendLine("- Questionnaire : ${trame.nom} (version ${trame.version})")
        sb.appendLine("- Référentiel : ${trame.source}")
        sb.appendLine("- Date : $date · durée : ${formatDuree(vm.secondes)}")
        sb.appendLine("- Participants : ${vm.participants.ifBlank { "non renseignés" }}")
        sb.appendLine("- Avancement : ${vm.traitees()} / ${vm.total()} questions traitées")
        sb.appendLine()
        sb.appendLine("## Questionnaire et réponses")
        sb.appendLine()

        var sectionCourante = ""
        trame.sections.forEach { section ->
            if (section.titre != sectionCourante) {
                sectionCourante = section.titre
                sb.appendLine("### $sectionCourante")
                sb.appendLine()
            }
            section.questions.forEach { q ->
                val etat = vm.etats[q.id] ?: Etat()
                val marque = when (etat.statut) {
                    EtatQuestion.REPONDUE -> "[x]"
                    EtatQuestion.EVOQUEE -> "[~]"
                    EtatQuestion.SANS_OBJET -> "[-]"
                    EtatQuestion.A_POSER -> "[ ]"
                }
                sb.appendLine("- $marque ${q.id} — ${q.question}")
                if (etat.statut == EtatQuestion.SANS_OBJET) sb.appendLine("      > sans objet")
                etat.citation?.let { c ->
                    sb.appendLine("      > « ${c.extrait} » (${c.horodatage}, confiance ${c.confiance} %, ${c.origine})")
                }
                if (etat.note.isNotBlank()) sb.appendLine("      > note : ${etat.note}")
                if (etat.statut == EtatQuestion.A_POSER || etat.statut == EtatQuestion.EVOQUEE) {
                    if (q.relance.isNotBlank()) sb.appendLine("      > relance à poser : ${q.relance}")
                }
            }
            sb.appendLine()
        }

        val reste = vm.resteAPoser()
        sb.appendLine("## Questions non traitées (${reste.size})")
        if (reste.isEmpty()) sb.appendLine("- aucune")
        reste.forEach { sb.appendLine("- ${it.id} — ${it.question}") }
        sb.appendLine()

        sb.appendLine("## Sujets évoqués à creuser")
        val creuser = vm.ecarts()
        if (creuser.isEmpty()) sb.appendLine("- aucun")
        creuser.forEach { sb.appendLine("- ${it.id} — ${it.question}") }
        sb.appendLine()

        sb.appendLine("## Actions à suivre")
        if (vm.actions.isEmpty()) sb.appendLine("- aucune")
        vm.actions.forEach { sb.appendLine("- ${it.intitule} (${it.questionId})") }
        sb.appendLine()

        sb.appendLine("## Transcription")
        if (vm.segments.isEmpty()) {
            sb.appendLine("_Aucune transcription (mode saisie manuelle ou transcription indisponible)._")
        } else {
            vm.segments.forEach { sb.appendLine("- [${it.horodatage}] ${it.intervenant} : ${it.texte}") }
        }
        return sb.toString()
    }

    fun formatDuree(secondes: Int): String =
        "%dh%02d".format(secondes / 3600, (secondes % 3600) / 60)

    fun nomFichier(vm: SeanceViewModel): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date())
        val dossier = vm.dossier.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return "compte-rendu-${dossier.ifBlank { "seance" }}-$date"
    }

    fun ecrireMarkdown(context: Context, nomBase: String, contenu: String): File {
        val repertoire = File(context.filesDir, "rapports").apply { mkdirs() }
        val fichier = File(repertoire, "$nomBase.md")
        fichier.writeText(contenu, Charsets.UTF_8)
        return fichier
    }

    fun ecrirePdf(context: Context, nomBase: String, contenu: String): File {
        val repertoire = File(context.filesDir, "rapports").apply { mkdirs() }
        val fichier = File(repertoire, "$nomBase.pdf")
        val document = PdfDocument()
        val titre = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val corps = Paint().apply { textSize = 9f }
        var page = document.startPage(PdfDocument.PageInfo.Builder(LARGEUR, HAUTEUR, 1).create())
        var y = MARGE
        var numeroPage = 1

        fun nouvellePage() {
            document.finishPage(page)
            numeroPage += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(LARGEUR, HAUTEUR, numeroPage).create())
            y = MARGE
        }

        contenu.lines().forEach { brute ->
            val ligne = brute.replace("«", "\"").replace("»", "\"")
            val pinceau = if (ligne.startsWith("#")) titre else corps
            val texte = ligne.replace(Regex("^#+\\s*"), "")
            pinceau.textSize = if (ligne.startsWith("###")) 10f else if (ligne.startsWith("##")) 11f else 9f
            val largeurUtile = LARGEUR - 2 * MARGE
            var reste = texte
            if (reste.isBlank()) {
                y += LIGNE
            } else {
                while (reste.isNotEmpty()) {
                    val coupe = pinceau.breakText(reste, true, largeurUtile, null)
                    if (coupe <= 0) break
                    if (y > HAUTEUR - MARGE) nouvellePage()
                    page.canvas.drawText(reste.substring(0, coupe), MARGE, y, pinceau)
                    y += LIGNE
                    reste = reste.substring(coupe).trimStart()
                }
            }
            if (y > HAUTEUR - MARGE) nouvellePage()
        }
        document.finishPage(page)
        fichier.outputStream().use { document.writeTo(it) }
        document.close()
        return fichier
    }

    fun uri(context: Context, fichier: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fichiers", fichier)

    fun partager(context: Context, sujet: String, fichiers: List<File>) {
        val uris = fichiers.map { uri(context, it) }
        val intention = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = if (fichiers.any { it.extension == "pdf" }) "application/pdf" else "text/plain"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_SUBJECT, sujet)
            putExtra(Intent.EXTRA_TEXT, sujet)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intention, "Partager le compte rendu"))
    }
}
