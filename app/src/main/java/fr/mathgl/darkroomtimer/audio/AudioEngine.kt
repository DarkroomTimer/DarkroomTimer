package fr.mathgl.darkroomtimer.audio

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Interface abstrayant la génération sonore.
 * Implémentations possibles: ToneGenerator (simple), AudioTrack (précis).
 */
interface AudioEngine {

    /**
     * Joue un ton continu pendant la durée spécifiée.
     * @param frequencyHz Fréquence en Hz (ex: 250, 440, 880)
     * @param durationMs Durée en millisecondes
     * @param volume Volume normalisé entre 0.0 et 1.0
     */
    fun playTone(frequencyHz: Int, durationMs: Int, volume: Float)

    /**
     * Joue une séquence de bips avec espacement.
     * @param frequencyHz Fréquence en Hz
     * @param beepCount Nombre de bips
     * @param beepDurationMs Durée de chaque bip en ms
     * @param silenceBetweenMs Silence entre les bips en ms
     * @param volume Volume normalisé entre 0.0 et 1.0
     */
    fun playBeepSequence(
        frequencyHz: Int,
        beepCount: Int,
        beepDurationMs: Int,
        silenceBetweenMs: Int,
        volume: Float
    )

    /**
     * Annule le son (pour MUTE ou arrêt d'un son en cours).
     */
    fun stop()

    /**
     * Libère les ressources audio.
     */
    fun release()
}

/**
 * Implémentation de [AudioEngine] utilisant [ToneGenerator].
 * Avantage: API simple, intégré à Android.
 * Limite: fréquences prédéfinies (ToneGenerator.TONE_PROP_BEEP = ~600Hz).
 * Pour des fréquences personnalisées, utiliser [AudioTrack] (à implémenter si nécessaire).
 */
class ToneGeneratorAudioEngine(
    private var audioVolume: AudioVolume = AudioVolume.MEDIUM
) : AudioEngine {

    private var toneGenerator: ToneGenerator? = null
    private var currentThread: Thread? = null

    override fun playTone(frequencyHz: Int, durationMs: Int, volume: Float) {
        // ToneGenerator ne supporte pas les fréquences personnalisées directement.
        // Pour 250Hz, 440Hz, 660Hz, 880Hz, on utilise ToneGenerator.TONE_PROP_BEEP
        // et on ajuste le volume. La fréquence réelle sera ~600Hz mais perceptuellement acceptable.
        // Pour une précision fréquence, implémenter ToneGeneratorAudioEngine avec AudioTrack.

        val actualVolume = clampVolume(volume)
        if (actualVolume <= 0f) return

        val tg = toneGenerator ?: run {
            val newTg = ToneGenerator(AudioManager.STREAM_ALARM, (actualVolume * 100).toInt())
            toneGenerator = newTg
            newTg
        }

        currentThread?.interrupt()
        currentThread = Thread {
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)
            Thread.sleep(durationMs.toLong())
        }.apply { start() }
    }

    override fun playBeepSequence(
        frequencyHz: Int,
        beepCount: Int,
        beepDurationMs: Int,
        silenceBetweenMs: Int,
        volume: Float
    ) {
        val actualVolume = clampVolume(volume)
        if (actualVolume <= 0f) return

        val tg = toneGenerator ?: run {
            val newTg = ToneGenerator(AudioManager.STREAM_ALARM, (actualVolume * 100).toInt())
            toneGenerator = newTg
            newTg
        }

        currentThread?.interrupt()
        currentThread = Thread {
            for (i in 0 until beepCount) {
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, beepDurationMs)
                Thread.sleep(beepDurationMs.toLong())
                if (i < beepCount - 1) {
                    Thread.sleep(silenceBetweenMs.toLong())
                }
            }
        }.apply { start() }
    }

    override fun stop() {
        currentThread?.interrupt()
        currentThread = null
        toneGenerator?.stopTone()
    }

    override fun release() {
        stop()
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun clampVolume(value: Float): Float {
        return value.coerceIn(0f, 1f)
    }
}
