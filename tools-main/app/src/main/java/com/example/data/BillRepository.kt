package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class BillRepository(private val dao: FoodBillDao) {

    val allBills: Flow<List<FoodBillUiModel>> = dao.getAllBills().map { list ->
        list.map { entity -> entity.toUiModel() }
    }

    val totalSpentAllTime: Flow<Double> = dao.getTotalSpentAllTime().map { it ?: 0.0 }

    suspend fun saveBill(
        id: Long = 0,
        dateString: String,
        timestamp: Long,
        purchaserName: String,
        centerName: String = "",
        subtitle: String = "",
        note: String,
        items: List<BillItem>,
        totalAmount: Double,
        billType: String = "market",
        showSignature: Boolean = true
    ): Long {
        val itemsJson = serializeItems(items)
        val entity = FoodBillEntity(
            id = id,
            dateString = dateString,
            timestamp = timestamp,
            purchaserName = purchaserName,
            centerName = centerName,
            subtitle = subtitle,
            note = note,
            totalAmount = totalAmount,
            itemsJson = itemsJson,
            billType = billType,
            showSignature = showSignature
        )
        return if (id == 0L) {
            dao.insertBill(entity)
        } else {
            dao.updateBill(entity)
            id
        }
    }

    suspend fun deleteBill(id: Long) {
        dao.deleteBillById(id)
    }

    suspend fun getBillById(id: Long): FoodBillUiModel? {
        val entity = dao.getBillById(id) ?: return null
        return entity.toUiModel()
    }

    private fun FoodBillEntity.toUiModel(): FoodBillUiModel {
        val parsedItems = deserializeItems(itemsJson)
        return FoodBillUiModel(
            id = id,
            dateString = dateString,
            timestamp = timestamp,
            purchaserName = purchaserName,
            centerName = centerName,
            subtitle = subtitle,
            note = note,
            totalAmount = totalAmount,
            items = parsedItems,
            billType = billType,
            showSignature = showSignature
        )
    }

    companion object {
        fun serializeItems(items: List<BillItem>): String {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("name", item.name)
                obj.put("quantity", item.quantity)
                obj.put("rate", item.rate)
                obj.put("amount", item.amount)
                array.put(obj)
            }
            return array.toString()
        }

        fun deserializeItems(json: String): List<BillItem> {
            if (json.isBlank()) return emptyList()
            return try {
                val array = JSONArray(json)
                val list = ArrayList<BillItem>(array.length())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        BillItem(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            name = obj.optString("name", ""),
                            quantity = obj.optString("quantity", ""),
                            rate = obj.optString("rate", "0"),
                            amount = obj.optDouble("amount", 0.0)
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

data class FoodBillUiModel(
    val id: Long,
    val dateString: String,
    val timestamp: Long,
    val purchaserName: String,
    val centerName: String = "",
    val subtitle: String = "",
    val note: String,
    val totalAmount: Double,
    val items: List<BillItem>,
    val billType: String = "market",
    val showSignature: Boolean = true
)
