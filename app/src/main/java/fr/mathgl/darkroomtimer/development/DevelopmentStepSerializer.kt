package fr.mathgl.darkroomtimer.development

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/**
 * Serialiseur JSON pour DevelopmentStep (sealed class).
 * Gère la sérialisation/désérialisation de BathStep et PauseStep.
 */
object DevelopmentStepSerializer {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(DevelopmentStep::class.java, DevelopmentStepTypeAdapter)
        .create()

    fun serializeSteps(steps: List<DevelopmentStep>): String {
        return gson.toJson(steps)
    }

    fun deserializeSteps(json: String): List<DevelopmentStep> {
        return try {
            gson.fromJson(json, Array<DevelopmentStep>::class.java).toList()
        } catch (e: JsonParseException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private object DevelopmentStepTypeAdapter : JsonDeserializer<DevelopmentStep>, JsonSerializer<DevelopmentStep> {

        private const val KEY_TYPE = "type"
        private const val TYPE_BATH = "BATH"
        private const val TYPE_PAUSE = "PAUSE"
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_DURATION_SECONDS = "durationSeconds"
        private const val KEY_PRE_END_ALERT_SECONDS = "preEndAlertSeconds"
        private const val KEY_ELAPSED_SECONDS = "elapsedSeconds"

        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): DevelopmentStep {
            val jsonObject = json as JsonObject
            val type = jsonObject.get(KEY_TYPE)?.asString ?: TYPE_BATH

            val id = context.deserialize<Int?>(jsonObject.get(KEY_ID), Int::class.java) ?: 0
            val name = context.deserialize<String?>(jsonObject.get(KEY_NAME), String::class.java) ?: ""
            val durationSeconds = context.deserialize<Int?>(jsonObject.get(KEY_DURATION_SECONDS), Int::class.java) ?: 0
            val elapsedSeconds = context.deserialize<Long?>(jsonObject.get(KEY_ELAPSED_SECONDS), Long::class.java) ?: 0L

            return when (type) {
                TYPE_BATH -> {
                    val preEndAlertSeconds = context.deserialize<Int?>(jsonObject.get(KEY_PRE_END_ALERT_SECONDS), Int::class.java) ?: 0
                    DevelopmentStep.BathStep(
                        id = id,
                        name = name,
                        durationSeconds = durationSeconds,
                        preEndAlertSeconds = preEndAlertSeconds,
                        elapsedSeconds = elapsedSeconds
                    )
                }
                TYPE_PAUSE -> {
                    DevelopmentStep.PauseStep(
                        id = id,
                        name = name,
                        durationSeconds = durationSeconds,
                        elapsedSeconds = elapsedSeconds
                    )
                }
                else -> {
                    DevelopmentStep.BathStep(id = id, name = name, durationSeconds = durationSeconds, elapsedSeconds = elapsedSeconds)
                }
            }
        }

        override fun serialize(
            src: DevelopmentStep,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val jsonObject = JsonObject()

            when (src) {
                is DevelopmentStep.BathStep -> {
                    jsonObject.add(KEY_TYPE, JsonPrimitive(TYPE_BATH))
                    jsonObject.add(KEY_ID, JsonPrimitive(src.id))
                    jsonObject.add(KEY_NAME, JsonPrimitive(src.name))
                    jsonObject.add(KEY_DURATION_SECONDS, JsonPrimitive(src.durationSeconds))
                    jsonObject.add(KEY_PRE_END_ALERT_SECONDS, JsonPrimitive(src.preEndAlertSeconds))
                    jsonObject.add(KEY_ELAPSED_SECONDS, JsonPrimitive(src.elapsedSeconds))
                }
                is DevelopmentStep.PauseStep -> {
                    jsonObject.add(KEY_TYPE, JsonPrimitive(TYPE_PAUSE))
                    jsonObject.add(KEY_ID, JsonPrimitive(src.id))
                    jsonObject.add(KEY_NAME, JsonPrimitive(src.name))
                    jsonObject.add(KEY_DURATION_SECONDS, JsonPrimitive(src.durationSeconds))
                    jsonObject.add(KEY_ELAPSED_SECONDS, JsonPrimitive(src.elapsedSeconds))
                }
            }

            return jsonObject
        }
    }
}
