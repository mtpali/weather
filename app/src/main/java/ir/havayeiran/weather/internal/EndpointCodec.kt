package ir.havayeiran.weather.internal

internal object EndpointCodec {
    private const val KEY = 0x5D

    private fun decode(values: IntArray): String = buildString(values.size) {
        values.forEachIndexed { index, value ->
            val mask = KEY xor ((index * 29 + 0x33) and 0xFF)
            append((value xor mask).toChar())
        }
    }

    fun instagramOneLabel(): String = decode(intArrayOf(3,98,82,190,150,252,146,215,47,11,105))
    fun instagramTwoLabel(): String = decode(intArrayOf(3,98,82,190,150,252,146,215,47,11,105,29))
    fun instagramThreeLabel(): String = decode(intArrayOf(3,98,82,190,150,252,146,215,47,11,105,78))

    fun instagramOneUrl(): String = decode(intArrayOf(6,121,68,167,137,163,147,140,49,18,127,1,187,159,231,207,63,26,18,102,71,231,143,252,219,122,21,112,96,72,168,142,160,217,57,25,123,22))
    fun instagramTwoUrl(): String = decode(intArrayOf(6,121,68,167,137,163,147,140,49,18,127,1,187,159,231,207,63,26,18,102,71,231,143,252,219,122,21,112,96,72,168,142,160,217,57,25,123,11,243))
    fun instagramThreeUrl(): String = decode(intArrayOf(6,121,68,167,137,163,147,140,49,18,127,1,187,159,231,207,63,26,18,102,71,231,143,252,219,122,21,112,96,72,168,142,160,217,57,25,123,88,243))
    fun developerUrl(): String = decode(intArrayOf(6,121,68,167,137,163,147,140,50,75,101,74,253,135,228,213,103,75,83))
}
