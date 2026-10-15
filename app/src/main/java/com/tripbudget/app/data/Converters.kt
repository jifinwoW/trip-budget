package com.tripbudget.app.data

import androidx.room.TypeConverter
import com.tripbudget.app.domain.ExpenseCategory
import com.tripbudget.app.domain.PaymentSource

class Converters {
    @TypeConverter
    fun fromPaymentSource(value: PaymentSource): String = value.name

    @TypeConverter
    fun toPaymentSource(value: String): PaymentSource = PaymentSource.valueOf(value)

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory = ExpenseCategory.valueOf(value)
}
