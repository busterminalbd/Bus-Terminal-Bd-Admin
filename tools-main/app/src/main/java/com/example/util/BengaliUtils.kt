package com.example.util

import java.text.DecimalFormat

object BengaliUtils {

    private val englishToBengaliDigitsMap = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )

    private val bengaliToEnglishDigitsMap = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    fun toBengaliDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            sb.append(englishToBengaliDigitsMap[ch] ?: ch)
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            sb.append(bengaliToEnglishDigitsMap[ch] ?: ch)
        }
        return sb.toString()
    }

    fun formatBengaliCurrency(amount: Double): String {
        val df = DecimalFormat("#,##0.##")
        val formattedNumber = df.format(amount)
        return toBengaliDigits(formattedNumber)
    }

    fun formatBengaliTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
        val formatted = sdf.format(java.util.Date(timestamp))
        return toBengaliDigits(formatted)
    }

    fun formatEnglishTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
        return sdf.format(java.util.Date(timestamp))
    }

    fun parseBengaliNumber(input: String): Double {
        val engString = toEnglishDigits(input).replace(",", "").trim()
        val direct = engString.toDoubleOrNull()
        if (direct != null) return direct

        val regex = Regex("""\d+(\.\d+)?""")
        val match = regex.find(engString)
        return match?.value?.toDoubleOrNull() ?: 0.0
    }

    fun parseBengaliInt(input: String): Int {
        return parseBengaliNumber(input).toInt()
    }

    private val bengaliNumbers0to99 = arrayOf(
        "শূন্য", "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়", "দশ",
        "এগারো", "বারো", "তেরো", "চৌদ্দ", "পনেরো", "ষোলো", "সতেরো", "আঠারো", "উনিশ", "বিশ",
        "একুশ", "বাইশ", "তেইশ", "চব্বিশ", "পঁচিশ", "ছাব্বিশ", "সাতাশ", "আঠাশ", "ঊনত্রিশ", "ত্রিশ",
        "একত্রিশ", "বত্রিশ", "তেত্রিশ", "চৌত্রিশ", "পঁয়ত্রিশ", "ছত্রিশ", "সাঁইত্রিশ", "আটত্রিশ", "ঊনচল্লিশ", "চল্লিশ",
        "একচল্লিশ", "বিয়াল্লিশ", "তেতাল্লিশ", "চুয়াল্লিশ", "পঁয়তাল্লিশ", "ছেচল্লিশ", "সাতচল্লিশ", "আটচল্লিশ", "ঊনপঞ্চাশ", "পঞ্চাশ",
        "একান্ন", "বায়ান্ন", "তিপ্পান্ন", "চুয়ান্ন", "পঞ্চান্ন", "ছাপ্পান্ন", "সাতান্ন", "আটান্ন", "ঊনষাট", "ষাট",
        "একষট্টি", "বাষট্টি", "তেষট্টি", "চৌষট্টি", "পঁয়ষট্টি", "ছেষট্টি", "সাতষট্টি", "আটষট্টি", "ঊনসত্তর", "সত্তর",
        "একাত্তর", "বাহাত্তর", "তিয়াত্তর", "চুয়াত্তর", "পঁচাত্তর", "ছিয়াত্তর", "সাতাত্তর", "আটাত্তর", "ঊনআশি", "আশি",
        "একাশি", "বিরাশি", "তিরাশি", "চুরাশি", "পঁচাশি", "ছিয়াশি", "সাতাশি", "আটাশি", "ঊননব্বই", "নব্বই",
        "একানব্বই", "বিরানব্বই", "তিরানব্বই", "চুরানব্বই", "পঁচানব্বই", "ছিয়ানব্বই", "সাতানব্বই", "আটানব্বই", "নিরানব্বই"
    )

    fun numberToBengaliWords(number: Long): String {
        if (number == 0L) return "শূন্য"
        if (number < 0) return "মাইনাস " + numberToBengaliWords(-number)

        var n = number
        val sb = StringBuilder()

        val crore = n / 10000000L
        n %= 10000000L
        if (crore > 0) {
            sb.append(numberToBengaliWords(crore)).append(" কোটি ")
        }

        val lakh = n / 100000L
        n %= 100000L
        if (lakh > 0) {
            sb.append(bengaliNumbers0to99[lakh.toInt()]).append(" লাখ ")
        }

        val thousand = n / 1000L
        n %= 1000L
        if (thousand > 0) {
            sb.append(bengaliNumbers0to99[thousand.toInt()]).append(" হাজার ")
        }

        val hundred = n / 100L
        n %= 100L
        if (hundred > 0) {
            sb.append(bengaliNumbers0to99[hundred.toInt()]).append(" শত ")
        }

        if (n > 0) {
            sb.append(bengaliNumbers0to99[n.toInt()])
        }

        return sb.toString().trim().replace(Regex("\\s+"), " ")
    }

    fun amountToBengaliWords(amount: Double): String {
        if (amount <= 0.0) return ""
        val taka = amount.toLong()
        val paisa = Math.round((amount - taka) * 100).toInt()

        val takaWords = if (taka > 0) "${numberToBengaliWords(taka)} টাকা" else ""
        val paisaWords = if (paisa > 0) "${bengaliNumbers0to99[paisa]} পয়সা" else ""

        return when {
            takaWords.isNotBlank() && paisaWords.isNotBlank() -> "$takaWords $paisaWords মাত্র"
            takaWords.isNotBlank() -> "$takaWords মাত্র"
            paisaWords.isNotBlank() -> "$paisaWords মাত্র"
            else -> ""
        }
    }

    fun formatPresetDisplayText(preset: QuickPreset): String {
        val name = preset.name.trim()
        val qty = preset.defaultQty.trim()
        val priceRaw = preset.defaultRate.ifBlank { preset.defaultAmount }.trim()

        val qtyFormatted = if (qty.isNotBlank()) toBengaliDigits(qty) else ""
        val priceFormatted = if (priceRaw.isNotBlank()) {
            "টাকা: ${toBengaliDigits(priceRaw)}৳"
        } else ""

        val infoPart = when {
            qtyFormatted.isNotBlank() && priceFormatted.isNotBlank() -> "$qtyFormatted | $priceFormatted"
            qtyFormatted.isNotBlank() -> qtyFormatted
            priceFormatted.isNotBlank() -> priceFormatted
            else -> ""
        }

        return if (infoPart.isNotBlank()) {
            "$name ($infoPart)"
        } else {
            name
        }
    }

    val defaultQuickPresets = emptyList<QuickPreset>()
}

data class QuickPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val defaultQty: String = "",
    val defaultRate: String = "",
    val defaultAmount: String = "",
    val isFolder: Boolean = false,
    val children: List<QuickPreset> = emptyList()
)

data class FolderOption(
    val id: String,
    val displayName: String,
    val level: Int = 0
)

fun List<QuickPreset>.getAllFlatItems(): List<QuickPreset> {
    val result = mutableListOf<QuickPreset>()
    for (preset in this) {
        if (preset.isFolder) {
            result.addAll(preset.children.getAllFlatItems())
        } else {
            result.add(preset)
        }
    }
    return result
}

fun List<QuickPreset>.getFolderOptions(
    excludeFolderId: String? = null,
    parentPath: String = "",
    level: Int = 0
): List<FolderOption> {
    val list = mutableListOf<FolderOption>()
    if (level == 0) {
        list.add(FolderOption(id = "", displayName = "🏠 প্রধান তালিকা (Main List)", level = 0))
    }
    for (node in this) {
        if (node.isFolder && node.id != excludeFolderId) {
            val currentPath = if (parentPath.isEmpty()) node.name else "$parentPath > ${node.name}"
            list.add(FolderOption(id = node.id, displayName = currentPath, level = level + 1))
            list.addAll(node.children.getFolderOptions(excludeFolderId, currentPath, level + 1))
        }
    }
    return list
}

fun List<QuickPreset>.findParentFolderId(nodeId: String): String? {
    for (node in this) {
        if (node.id == nodeId) return ""
        if (node.isFolder) {
            if (node.children.any { it.id == nodeId }) return node.id
            val subParent = node.children.findParentFolderId(nodeId)
            if (subParent != null) return subParent
        }
    }
    return null
}

fun List<QuickPreset>.removeNodeById(nodeId: String): List<QuickPreset> {
    val result = mutableListOf<QuickPreset>()
    for (node in this) {
        if (node.id == nodeId || (!node.isFolder && node.name.equals(nodeId, ignoreCase = true))) {
            continue
        }
        if (node.isFolder) {
            val updatedChildren = node.children.removeNodeById(nodeId)
            result.add(node.copy(children = updatedChildren))
        } else {
            result.add(node)
        }
    }
    return result
}

fun List<QuickPreset>.addNodeToParent(node: QuickPreset, targetFolderId: String?): List<QuickPreset> {
    if (targetFolderId.isNullOrBlank() || targetFolderId == "ROOT") {
        return listOf(node) + this
    }
    return this.map { current ->
        if (current.isFolder && current.id == targetFolderId) {
            current.copy(children = listOf(node) + current.children)
        } else if (current.isFolder) {
            current.copy(children = current.children.addNodeToParent(node, targetFolderId))
        } else {
            current
        }
    }
}

fun List<QuickPreset>.updateOrMoveNode(
    nodeId: String,
    updatedNode: QuickPreset,
    targetFolderId: String?
): List<QuickPreset> {
    val treeWithoutNode = this.removeNodeById(nodeId)
    return treeWithoutNode.addNodeToParent(updatedNode, targetFolderId)
}

fun List<QuickPreset>.reorderNodesInParent(
    parentFolderId: String?,
    fromIndex: Int,
    toIndex: Int
): List<QuickPreset> {
    if (parentFolderId.isNullOrBlank() || parentFolderId == "ROOT") {
        if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this
        val mutable = this.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        return mutable
    }
    return this.map { node ->
        if (node.isFolder && node.id == parentFolderId) {
            val children = node.children.toMutableList()
            if (fromIndex in children.indices && toIndex in children.indices && fromIndex != toIndex) {
                val moved = children.removeAt(fromIndex)
                children.add(toIndex, moved)
            }
            node.copy(children = children)
        } else if (node.isFolder) {
            node.copy(children = node.children.reorderNodesInParent(parentFolderId, fromIndex, toIndex))
        } else {
            node
        }
    }
}

fun parseQuickPresetFromJsonObject(obj: org.json.JSONObject): QuickPreset {
    val id = obj.optString("id", "").ifBlank { java.util.UUID.randomUUID().toString() }
    val name = obj.optString("name", "")
    val defaultQty = obj.optString("defaultQty", "")
    val defaultRate = obj.optString("defaultRate", "")
    val defaultAmount = obj.optString("defaultAmount", "")
    val isFolder = obj.optBoolean("isFolder", false)
    val childrenList = mutableListOf<QuickPreset>()
    val childrenArray = obj.optJSONArray("children")
    if (childrenArray != null) {
        for (i in 0 until childrenArray.length()) {
            val childObj = childrenArray.optJSONObject(i)
            if (childObj != null) {
                val child = parseQuickPresetFromJsonObject(childObj)
                if (child.name.isNotBlank()) {
                    childrenList.add(child)
                }
            }
        }
    }
    return QuickPreset(
        id = id,
        name = name,
        defaultQty = defaultQty,
        defaultRate = defaultRate,
        defaultAmount = defaultAmount,
        isFolder = isFolder,
        children = childrenList
    )
}

fun quickPresetToJsonObject(preset: QuickPreset): org.json.JSONObject {
    val obj = org.json.JSONObject()
    obj.put("id", preset.id)
    obj.put("name", preset.name)
    obj.put("defaultQty", preset.defaultQty)
    obj.put("defaultRate", preset.defaultRate)
    obj.put("defaultAmount", preset.defaultAmount)
    obj.put("isFolder", preset.isFolder)
    if (preset.isFolder) {
        val array = org.json.JSONArray()
        preset.children.forEach { child ->
            array.put(quickPresetToJsonObject(child))
        }
        obj.put("children", array)
    }
    return obj
}
