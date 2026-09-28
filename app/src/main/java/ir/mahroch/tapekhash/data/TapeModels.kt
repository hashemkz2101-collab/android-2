package ir.mahroch.tapekhash.data

import org.json.JSONObject

data class TapeRow(
    val id: Int,
    val box: String,
    val tape: String,
    val imageUrl: String,
    val imageUrls: List<String>,
    val status: String,
    val holderUsername: String,
    val holderName: String,
    val isMine: Boolean,
    val note: String
) {
    companion object {
        fun fromJson(o: JSONObject): TapeRow {
            val urls = mutableListOf<String>()
            val arr = o.optJSONArray("imageUrls")
            if (arr != null) for (i in 0 until arr.length()) urls.add(arr.getString(i))
            return TapeRow(
                id = o.optInt("row"),
                box = o.optString("box"),
                tape = o.optString("tape"),
                imageUrl = o.optString("imageUrl"),
                imageUrls = urls,
                status = o.optString("status"),
                holderUsername = o.optString("holderUsername"),
                holderName = o.optString("holderName"),
                isMine = o.optBoolean("isMine"),
                note = o.optString("note")
            )
        }
    }
}
