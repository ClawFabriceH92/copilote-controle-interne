package fr.fh.copiloteci

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

private enum class Ecran { ACCUEIL, SEANCE, REVUE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ThemeCopilote { Application() } }
    }
}

@Composable
private fun Application() {
    val context = LocalContext.current
    val vm: SeanceViewModel = viewModel()
    var ecran by remember { mutableStateOf(Ecran.ACCUEIL) }
    var questionAudio by remember { mutableStateOf(false) }
    var conserverAudio by remember { mutableStateOf<Boolean?>(null) }

    val enregistreur = remember { Enregistreur(context) }
    var transcription by remember { mutableStateOf<Transcription?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { accordee ->
        if (accordee && vm.trame != null) {
            val fichier = enregistreur.demarrer("seance-${System.currentTimeMillis()}")
            if (fichier != null) {
                val t = Transcription(context) { texte, final -> vm.ajouterSegment("Intervenant", texte, final) }
                transcription = t
                if (t.disponible) {
                    t.demarrer()
                    vm.activerTranscription(true)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            vm.tic()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            transcription?.arreter()
            enregistreur.arreter()
        }
    }

    when (ecran) {
        Ecran.ACCUEIL -> EcranAccueil(vm) {
            val accord = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (accord) {
                val fichier = enregistreur.demarrer("seance-${System.currentTimeMillis()}")
                if (fichier != null) {
                    val t = Transcription(context) { texte, final -> vm.ajouterSegment("Intervenant", texte, final) }
                    transcription = t
                    if (t.disponible) {
                        t.demarrer()
                        vm.activerTranscription(true)
                    }
                }
            } else {
                permission.launch(Manifest.permission.RECORD_AUDIO)
            }
            ecran = Ecran.SEANCE
        }

        Ecran.SEANCE -> EcranSeance(
            vm = vm,
            transcriptionDisponible = transcription?.disponible ?: true,
            onBasculerMicro = {
                val t = transcription
                if (t != null && vm.transcriptionActive) {
                    t.arreter()
                    vm.activerTranscription(false)
                } else {
                    val nouveau = t ?: Transcription(context) { texte, final -> vm.ajouterSegment("Intervenant", texte, final) }
                    transcription = nouveau
                    nouveau.demarrer()
                    vm.activerTranscription(true)
                }
            },
            onSegmentManuel = { vm.ajouterSegment("Saisie manuelle", it, true) },
            onTerminer = {
                transcription?.arreter()
                vm.activerTranscription(false)
                enregistreur.arreter()
                questionAudio = true
            }
        )

        Ecran.REVUE -> EcranRevue(
            vm = vm,
            cheminAudio = if (conserverAudio == true) enregistreur.chemin()?.absolutePath else null,
            onExporter = {
                val contenu = Rapport.texte(vm)
                val nom = Rapport.nomFichier(vm)
                val md = Rapport.ecrireMarkdown(context, nom, contenu)
                val pdf = Rapport.ecrirePdf(context, nom, contenu)
                Rapport.partager(context, "Compte rendu de contrôle interne — ${vm.dossier}", listOf(pdf, md))
            },
            onNouvelleSeance = {
                enregistreur.supprimer()
                vm.reinitialiser()
                conserverAudio = null
                ecran = Ecran.ACCUEIL
            }
        )
    }

    if (questionAudio) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Enregistrement audio de la séance") },
            text = {
                Text(
                    "Que fait-on de l'enregistrement ? Il sert de preuve de ce qui a été dit " +
                        "pendant l'entretien.\n\n" +
                        "Conserver : le fichier reste dans l'application, attaché au dossier.\n" +
                        "Effacer : l'enregistrement est supprimé tout de suite, la transcription est conservée."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    conserverAudio = true
                    questionAudio = false
                    ecran = Ecran.REVUE
                }) { Text("Conserver") }
            },
            dismissButton = {
                TextButton(onClick = {
                    enregistreur.supprimer()
                    conserverAudio = false
                    questionAudio = false
                    ecran = Ecran.REVUE
                }) { Text("Effacer") }
            }
        )
    }
}
