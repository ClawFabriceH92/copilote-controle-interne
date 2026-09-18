package fr.fh.copiloteci

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranSeance(
    vm: SeanceViewModel,
    transcriptionDisponible: Boolean,
    onBasculerMicro: () -> Unit,
    onSegmentManuel: (String) -> Unit,
    onTerminer: () -> Unit
) {
    val trame = vm.trame ?: return
    var onglet by remember { mutableIntStateOf(0) }
    var saisieManuelle by remember { mutableStateOf("") }
    var noteOuverte by remember { mutableStateOf<String?>(null) }
    var actionOuverte by remember { mutableStateOf<Question?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            vm.dossier.ifBlank { "Séance" },
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${trame.nom} · ${Rapport.formatDuree(vm.secondes)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onBasculerMicro, enabled = transcriptionDisponible) {
                        Icon(
                            if (vm.transcriptionActive) Icons.Filled.Mic else Icons.Filled.MicOff,
                            contentDescription = if (vm.transcriptionActive) "Couper la transcription" else "Activer la transcription",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { vm.basculerEnregistrement() }) {
                        Icon(
                            if (vm.enregistrement) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (vm.enregistrement) "Suspendre" else "Reprendre",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${vm.resteAPoser().size} questions restantes",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${vm.traitees()} / ${vm.total()} traitées · ${vm.ecarts().size} à creuser",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = onTerminer) { Text("Terminer") }
                }
            }
        }
    ) { marge ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(marge)
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { vm.progression() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
                Spacer(Modifier.height(12.dp))

                val suggeree = vm.questionSuggeree()
                if (suggeree != null) {
                    val etat = vm.etats[suggeree.id] ?: Etat()
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (etat.statut == EtatQuestion.EVOQUEE)
                                Charte.ambreFond else MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                if (etat.statut == EtatQuestion.EVOQUEE) "À CREUSER — ${suggeree.id}" else "QUESTION À POSER — ${suggeree.id}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (etat.statut == EtatQuestion.EVOQUEE) Charte.ambre else Charte.bleu
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                suggeree.question,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (suggeree.attendu.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Réponse attendue : ${suggeree.attendu}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            etat.citation?.let { c ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Entendu : « ${c.extrait} »",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (etat.statut == EtatQuestion.EVOQUEE && suggeree.relance.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Relance : « ${suggeree.relance} »",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Charte.ambre
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.marquer(suggeree.id, EtatQuestion.REPONDUE) }) {
                                    Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Répondue")
                                }
                                OutlinedButton(onClick = { actionOuverte = suggeree }) {
                                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Action")
                                }
                                IconButton(onClick = { vm.marquer(suggeree.id, EtatQuestion.SANS_OBJET) }) {
                                    Icon(
                                        Icons.Filled.RemoveCircleOutline,
                                        "Sans objet",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            TabRow(selectedTabIndex = onglet) {
                Tab(selected = onglet == 0, onClick = { onglet = 0 }, text = { Text("Questions") })
                Tab(
                    selected = onglet == 1,
                    onClick = { onglet = 1 },
                    text = { Text("Transcription (${vm.segments.size})") }
                )
            }

            if (onglet == 0) {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    trame.sections.forEach { section ->
                        item {
                            Text(
                                section.titre,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                            )
                        }
                        items(section.questions, key = { it.id }) { question ->
                            CarteQuestion(
                                question = question,
                                etat = vm.etats[question.id] ?: Etat(),
                                surMarquer = { vm.marquer(question.id, it) },
                                surNoter = { noteOuverte = question.id },
                                surAction = { actionOuverte = question }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    if (!transcriptionDisponible) {
                        item {
                            Text(
                                "Transcription vocale indisponible sur cet appareil : " +
                                    "saisis les échanges à la main, la détection des questions fonctionne pareil.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                    items(vm.segments) { segment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "${segment.intervenant} · ${segment.horodatage}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(segment.texte, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    item {
                        Column(Modifier.padding(top = 10.dp, bottom = 20.dp)) {
                            OutlinedTextField(
                                value = saisieManuelle,
                                onValueChange = { saisieManuelle = it },
                                label = { Text("Ajouter un échange à la main") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onSegmentManuel(saisieManuelle)
                                    saisieManuelle = ""
                                },
                                enabled = saisieManuelle.isNotBlank()
                            ) { Text("Ajouter") }
                        }
                    }
                }
            }
        }
    }

    noteOuverte?.let { id ->
        val etat = vm.etats[id] ?: Etat()
        var texte by remember(id) { mutableStateOf(etat.note) }
        AlertDialog(
            onDismissRequest = { noteOuverte = null },
            title = { Text("Note — $id") },
            text = {
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    label = { Text("Observation du collaborateur") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.noter(id, texte)
                    noteOuverte = null
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { noteOuverte = null }) { Text("Annuler") } }
        )
    }

    actionOuverte?.let { question ->
        var texte by remember(question.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { actionOuverte = null },
            title = { Text("Action à suivre") },
            text = {
                Column {
                    Text(
                        question.question,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = texte,
                        onValueChange = { texte = it },
                        label = { Text("Ce qu'il faut faire, et par qui") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.ajouterAction(texte, question.id)
                    actionOuverte = null
                }) { Text("Ajouter") }
            },
            dismissButton = { TextButton(onClick = { actionOuverte = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun CarteQuestion(
    question: Question,
    etat: Etat,
    surMarquer: (EtatQuestion) -> Unit,
    surNoter: () -> Unit,
    surAction: () -> Unit
) {
    val fond = when (etat.statut) {
        EtatQuestion.REPONDUE -> Charte.vertFond
        EtatQuestion.EVOQUEE -> Charte.ambreFond
        EtatQuestion.SANS_OBJET -> MaterialTheme.colorScheme.surfaceVariant
        EtatQuestion.A_POSER -> MaterialTheme.colorScheme.surface
    }
    val couleurEtat = when (etat.statut) {
        EtatQuestion.REPONDUE -> Charte.vert
        EtatQuestion.EVOQUEE -> Charte.ambre
        EtatQuestion.SANS_OBJET -> MaterialTheme.colorScheme.onSurfaceVariant
        EtatQuestion.A_POSER -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = fond),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    question.id,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    question.question,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(color = fond, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, couleurEtat)) {
                    Text(
                        etat.statut.libelle,
                        style = MaterialTheme.typography.labelSmall,
                        color = couleurEtat,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            etat.citation?.let { c ->
                Spacer(Modifier.height(6.dp))
                Text(
                    "« ${c.extrait} »",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${c.horodatage} · confiance ${c.confiance} % · ${c.origine}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (etat.note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "note : ${etat.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (etat.statut == EtatQuestion.A_POSER) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "attendu : ${question.attendu}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (etat.statut != EtatQuestion.REPONDUE) {
                    TextButton(onClick = { surMarquer(EtatQuestion.REPONDUE) }) { Text("Répondue") }
                }
                if (etat.statut != EtatQuestion.A_POSER) {
                    TextButton(onClick = { surMarquer(EtatQuestion.A_POSER) }) { Text("Rouvrir") }
                }
                TextButton(onClick = { surMarquer(EtatQuestion.SANS_OBJET) }) { Text("Sans objet") }
                TextButton(onClick = surNoter) { Text("Note") }
                TextButton(onClick = surAction) { Text("Action") }
            }
            if (question.pieces.isNotEmpty()) {
                Text(
                    "pièces : ${question.pieces.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
