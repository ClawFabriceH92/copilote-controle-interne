package fr.fh.copiloteci

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranAccueil(vm: SeanceViewModel, onDemarrer: () -> Unit) {
    val context = LocalContext.current
    val trames = remember { Trames.lister(context) }
    var dossier by remember { mutableStateOf("") }
    var participants by remember { mutableStateOf("") }
    var choisie by remember { mutableStateOf<Trame?>(trames.firstOrNull()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contrôle interne", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { marge ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(marge)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Préparation de la séance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Tout reste sur cet appareil : audio, transcription et compte rendu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = dossier,
                onValueChange = { dossier = it },
                label = { Text("Dossier client") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = participants,
                onValueChange = { participants = it },
                label = { Text("Participants (interlocuteurs du client)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(22.dp))

            Text(
                "Questionnaire",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            trames.forEach { trame ->
                val active = trame.id == choisie?.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { choisie = trame },
                    colors = CardDefaults.cardColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        if (active) 2.dp else 1.dp,
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(trame.nom, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${trame.nbQuestions} questions · ${trame.sections.size} sections · environ ${trame.dureeCibleMin} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (trame.source.isNotBlank()) {
                            Text(
                                trame.source,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val trame = choisie ?: return@Button
                    vm.demarrer(trame, dossier, participants)
                    onDemarrer()
                },
                enabled = choisie != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Démarrer la séance")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Pendant l'entretien : la transcription coche les questions abordées, " +
                    "et l'écran garde sous les yeux celles qui restent à poser.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
