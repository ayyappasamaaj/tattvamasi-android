package com.ayyappasamaaj.tattvamasi.model

class ParentListItem(name: String) {
    var name: String? = null

    init {
        this.name = toTitleCase(name)
    }

    companion object {
        private fun toTitleCase(givenString: String): String {
            val arr = givenString.split(" ").toTypedArray()
            val sb = StringBuffer()
            for (i in arr.indices) {
                sb.append(Character.toUpperCase(arr[i][0]))
                    .append(arr[i].substring(1)).append(" ")
            }
            return sb.toString().trim { it <= ' ' }
        }
    }
}