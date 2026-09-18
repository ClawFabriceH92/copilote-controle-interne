package fr.fh.copiloteci

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Chargement des trames embarquées : fichiers .json du dossier assets/trames, générés depuis les YAML du dépôt. */
object Trames {

    fun lister(context: Context): List<Trame> {
        val assets = context.assets.list("trames")?.filter { it.endsWith(".json") }?.sorted() ?: emptyList()
        return assets.mapNotNull { nom ->
            runCatching { lire(context, nom) }.getOrNull()
        }
    }

    fun lire(context: Context, fichier: String): Trame {
        val json = context.assets.open("trames/$fichier").bufferedReader().use { it.readText() }
        return depuisJson(JSONObject(json))
    }

    fun depuisJson(o: JSONObject): Trame {
        val sections = mutableListOf<Section>()
        val sectionsJson: JSONArray = o.optJSONArray("sections") ?: JSONArray()
        for (i in 0 until sectionsJson.length()) {
            val s = sectionsJson.getJSONObject(i)
            val questions = mutableListOf<Question>()
            val questionsJson: JSONArray = s.optJSONArray("questions") ?: JSONArray()
            for (j in 0 until questionsJson.length()) {
                val q = questionsJson.getJSONObject(j)
                questions += Question(
                    id = q.optString("id"),
                    question = q.optString("question"),
                    attendu = q.optString("attendu"),
                    relance = q.optString("relance"),
                    motsCles = q.optJSONArray("motsCles").toStringList(),
                    pieces = q.optJSONArray("pieces").toStringList()
                )
            }
            sections += Section(s.optString("titre"), questions)
        }
        return Trame(
            id = o.optString("id"),
            nom = o.optString("nom"),
            version = o.optString("version"),
            source = o.optString("source"),
            dureeCibleMin = o.optInt("dureeCibleMin", 45),
            participantsAttendus = o.optJSONArray("participantsAttendus").toStringList(),
            sections = sections
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { optString(it) }.filter { it.isNotBlank() }
    }
}
