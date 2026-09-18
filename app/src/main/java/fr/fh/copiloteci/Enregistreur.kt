package fr.fh.copiloteci

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Enregistrement audio de la séance (AAC dans un fichier .m4a du stockage privé). */
class Enregistreur(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var fichier: File? = null

    val enCours: Boolean get() = recorder != null

    fun demarrer(nomBase: String): File? {
        if (recorder != null) return fichier
        val repertoire = File(context.filesDir, "seances").apply { mkdirs() }
        val cible = File(repertoire, "$nomBase.m4a")
        return runCatching {
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
            else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(16_000)
            r.setAudioEncodingBitRate(64_000)
            r.setOutputFile(cible.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            fichier = cible
            cible
        }.getOrElse { e ->
            android.util.Log.w("CopiloteCI", "enregistrement impossible : ${e.message}")
            recorder = null
            fichier = null
            null
        }
    }

    fun arreter(): File? {
        val r = recorder ?: return fichier
        runCatching { r.stop() }
        runCatching { r.release() }
        recorder = null
        return fichier
    }

    fun supprimer(): Boolean {
        val f = fichier ?: return false
        recorder = null
        fichier = null
        return f.delete()
    }

    fun chemin(): File? = fichier
}
