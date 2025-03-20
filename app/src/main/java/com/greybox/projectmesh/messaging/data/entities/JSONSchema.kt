package com.greybox.projectmesh.messaging.data.entities

import org.json.JSONObject
import org.json.JSONException

class JSONSchema {

    private val schemaString = """
    {
        "type": "object",
        "required": ["id", "fromHost", "name", "size", "deviceName"],
        "properties": {
            "id": { "type": "integer" },
            "fromHost": { "type": "InetAddress" },
            "name": { "type": "string" },
            "size": { "type": "integer" },
            "deviceName": { "type": "string" }
        }
    }
    """

    fun schemaValidation(json: String): Boolean {
        try {
            val schemaJson = JSONObject(schemaString)
            val jsonObject = JSONObject(json)

            validate(jsonObject, schemaJson)
            return true
        }catch (e: JSONException) {
            println("Validation Error: ${e.message}")
            return false
        }
    }

    private fun validate(json: JSONObject, schema: JSONObject) {
        val requiredFields = schema.getJSONArray("required")
        val i = 0
        while (i < requiredFields.length()) {
            val field = requiredFields.getString(i)
            if (!json.has(field)) {
                throw JSONException("Missing required field: $field")
            }
        }
    }
}