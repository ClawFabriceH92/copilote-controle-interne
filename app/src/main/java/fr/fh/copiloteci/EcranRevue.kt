package fr.fh.copiloteci

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranRevue(
    vm: SeanceViewModel,
    cheminAudio: String?,
    onExporter: () -> Unit,
    onNouvelleSeance: () -> Unit
) {
    val trame = vm.trame ?: return
    val reste = vm.resteAPoser()
    val creuser = vm.ecarts()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revue de séance", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { marge ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(marge)
                .padding(16.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            vm.dossier.ifBlank { "Dossier non renseigné" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${trame.nom} · ${Rapport.formatDuree(vm.secondes)} · ${vm.segments.size} échanges transcrits",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${vm.traitees()} questions traitées · ${reste.size} non traitées · ${creuser.size} à creuser",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (cheminAudio != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Enregistrement audio : ${cheminAudio.substringAfterLast('/')}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            item {
                TitreListe("Questions non traitées (${reste.size})")
            }
            if (reste.isEmpty()) {
                item { TexteVide("Aucune : toutes les questions ont été traitées.") }
            }
            items(reste) { q ->
                LigneRevue(q.id, q.question, "relance : ${q.relance}", MaterialTheme.colorScheme.error)
            }

            item { TitreListe("Sujets évoqués à creuser (${creuser.size})") }
            if (creuser.isEmpty()) {
                item { TexteVide("Aucun sujet en suspens.") }
            }
            items(creuser) { q ->
                LigneRevue(q.id, q.question, "relance : ${q.relance}", Charte.ambre)
            }

            item { TitreListe("Actions à suivre (${vm.actions.size})") }
            if (vm.actions.isEmpty()) {
                item { TexteVide("Aucune action notée pendant la séance.") }
            }
            items(vm.actions) { a ->
                LigneRevue(a.questionId, a.intitule, "", MaterialTheme.colorScheme.primary)
            }

            item {
                Spacer(Modifier.height(18.dp))
                Button(onClick = onExporter, modifier = Modifier.fillMaxWidth()) {
                    Text("Générer et partager le compte rendu")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Le compte rendu reprend chaque question avec la citation qui l'a validée, " +
                        "les questions non traitées, les actions et la transcription complète.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onNouvelleSeance, modifier = Modifier.fillMaxWidth()) {
                    Text("Nouvelle séance")
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun TitreListe(texte: String) {
    Text(
        texte,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun TexteVide(texte: String) {
    Text(
        texte,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LigneRevue(id: String, texte: String, detail: String, couleur: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp)) {
            Text(
                id,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = couleur
            )
            Spacer(Modifier.height(0.dp))
            Column(Modifier.padding(start = 10.dp)) {
                Text(texte, style = MaterialTheme.typography.bodyMedium)
                if (detail.isNotBlank()) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
