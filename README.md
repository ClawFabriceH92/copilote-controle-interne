# Copilote Contrôle Interne

Application d'assistance **en direct** pour un collaborateur débutant qui conduit une procédure de contrôle interne (entretien, revue de procédure, tests).

Pendant la séance, l'application :

1. **transcrit la conversation au fil de l'eau** (STT local, aucune donnée qui sort du poste) ;
2. **coche automatiquement** les questions du questionnaire dès qu'elles sont traitées, en conservant la **citation horodatée** qui l'a justifié ;
3. **montre ce qui reste à poser**, section par section, avec une **relance suggérée** quand une question est restée sans réponse ;
4. **produit le compte rendu** de séance (réponses extraites, points non traités, actions à suivre, audio joint).

Public visé : collaborateurs juniors du cabinet, pour qui le questionnaire est autant une **checklist de non-oubli** qu'un **coach de formulation**.

## Principe de conception

- **L'humain garde la main.** L'IA propose (question évoquée, réponse entendue), le collaborateur valide d'un tap. Toute coche automatique est réversible et tracée.
- **Local d'abord.** Audio, transcription et analyse restent sur l'appareil (whisper.cpp + LLM local). Secret professionnel : rien ne part dans le cloud par défaut.
- **Traçabilité probante.** Chaque question cochée stocke l'horodatage, la citation exacte et le numéro de segment, plus l'empreinte SHA-256 de l'enregistrement.

## Périmètre v1 (à valider)

| Bloc | Contenu v1 |
|---|---|
| Questionnaires | format YAML versionné, sections, questions, relances types, mots-clés ; import depuis un fichier ou depuis le repo |
| Séance | enregistrement + transcription live, chrono, pause, marqueurs ⚑, reprise après interruption |
| Appariement | détection « question traitée » par mots-clés + embeddings + validation LLM local, seuil de confiance |
| Suivi | 4 états par question (à poser, évoquée, répondue, sans objet), progression, reste à poser, question suggérée |
| Rapport | Word/PDF : questionnaire commenté, réponses extraites avec citations, questions non traitées, actions, durée, empreinte audio |
| Sécurité | chiffrement au repos, PIN/biométrie, purge selon la politique de rétention du cabinet |

Hors périmètre v1 : multi-utilisateurs, synchronisation serveur, signature électronique, analyse statistique des campagnes.

## Architecture envisagée

- **Socle repris de [`transcripto-stream`](https://github.com/ClawFabriceH92/transcripto-stream)** : whisper.cpp en temps réel (Silero VAD), segments horodatés, dossiers clients, chiffrement AES-256-GCM, exports Word/PDF sans bibliothèque tierce. C'est du code déjà éprouvé, on ne réécrit pas le moteur audio.
- **Nouveau moteur (ce dépôt)** : `QuestionnaireEngine` (chargement YAML, état des questions), `Apparieur` (3 niveaux : mots-clés → embeddings → LLM), `RapportSeance`, `EcranSeance`.

```
questionnaires/*.yaml ──► QuestionnaireEngine ──► EtatQuestions (à poser/évoquée/répondue/N-A)
                                   ▲
transcription live (whisper) ──► Apparieur ──► citations horodatées + confiance
                                   │
                                   └──► RapportSeance (Word/PDF + citations + reste à poser)
```

## Maquette d'interface

`docs/maquette.html` : maquette cliquable (transcription simulée, questions qui se cochent, reste à poser, relance suggérée). Ouvrir le fichier dans un navigateur.

## État du projet

Repo initialisé le 18/09/2026. Spécification détaillée : `docs/SPEC.md`. Aucun code applicatif avant validation du périmètre et de l'interface.
