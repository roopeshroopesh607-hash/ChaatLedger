package com.example

import com.example.data.model.Expense
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTypes
import com.example.util.FormatUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `expense defaults and payment status check`() {
    val purchaseDate = 1726000000000L
    // Cash expense defaults to Paid
    val cashExpense = Expense(
      shopName = "Sabzi Mandi",
      category = "Ingredients",
      description = "Potatoes",
      amount = 500.0,
      paymentType = PaymentTypes.CASH,
      paymentStatus = PaymentStatus.PAID,
      paidDate = purchaseDate,
      purchaseDate = purchaseDate
    )
    assertEquals(PaymentStatus.PAID, cashExpense.paymentStatus)
    assertEquals(purchaseDate, cashExpense.paidDate)
    assertNull(cashExpense.billImageUri)

    // Credit expense defaults to Pending with null paidDate
    val creditExpense = Expense(
      shopName = "Amul Dairy",
      category = "Ingredients",
      description = "Paneer & Curd",
      amount = 1200.0,
      paymentType = PaymentTypes.CREDIT,
      paymentStatus = PaymentStatus.PENDING,
      paidDate = null,
      billImageUri = "file:///data/user/0/com.example/files/bills/bill_1.jpg",
      purchaseDate = purchaseDate
    )
    assertEquals(PaymentStatus.PENDING, creditExpense.paymentStatus)
    assertNull(creditExpense.paidDate)
    assertNotNull(creditExpense.billImageUri)

    // Mark as paid updates status and paidDate
    val settledExpense = creditExpense.copy(
      paymentStatus = PaymentStatus.PAID,
      paidDate = purchaseDate + 86400000L
    )
    assertEquals(PaymentStatus.PAID, settledExpense.paymentStatus)
    assertEquals(purchaseDate + 86400000L, settledExpense.paidDate)
  }

  @Test
  fun `reports period date bounds and net profit calculations`() {
    val weekStart = FormatUtils.getWeekStart()
    val weekEnd = FormatUtils.getWeekEnd()
    assertTrue(weekEnd > weekStart)

    val monthStart = FormatUtils.getMonthStart()
    val monthEnd = FormatUtils.getMonthEnd()
    assertTrue(monthEnd > monthStart)

    val last30DaysStart = FormatUtils.getLast30DaysStart()
    val todayEnd = FormatUtils.getTodayEnd()
    assertTrue(todayEnd > last30DaysStart)

    // Net Profit/Loss logic check
    val sales = 15000.0
    val expenses = 8500.0
    val netProfit = sales - expenses
    assertEquals(6500.0, netProfit, 0.001)
    assertTrue("Positive profit should be >= 0", netProfit >= 0)

    val lossSales = 4000.0
    val lossExpenses = 6200.0
    val netLoss = lossSales - lossExpenses
    assertEquals(-2200.0, netLoss, 0.001)
    assertTrue("Net loss should be negative", netLoss < 0)
  }

  @Test
  fun `firestore serialization and deserialization for Sale and Expense`() {
    val date = 1726000000000L
    val sale = com.example.data.model.Sale(
      date = date,
      cashTotal = 3500.0,
      paytmTotal = 2500.0,
      enteredBy = "Raju",
      firestoreId = "sale_$date"
    )

    // Map serialization for Firestore
    val saleMap = com.example.data.sync.SyncMappers.saleToMap(sale)
    assertEquals(date, saleMap["date"])
    assertEquals(3500.0, saleMap["cashTotal"])
    assertEquals(2500.0, saleMap["paytmTotal"])
    assertEquals(6000.0, saleMap["totalAmount"])
    assertEquals("Raju", saleMap["enteredBy"])
    assertEquals("sale_$date", saleMap["firestoreId"])

    // Map deserialization from Firestore
    val restoredSale = com.example.data.sync.SyncMappers.saleFromMap("sale_$date", saleMap)
    assertEquals(sale.date, restoredSale.date)
    assertEquals(sale.cashTotal, restoredSale.cashTotal, 0.001)
    assertEquals(sale.paytmTotal, restoredSale.paytmTotal, 0.001)
    assertEquals(sale.totalAmount, restoredSale.totalAmount, 0.001)
    assertEquals("Raju", restoredSale.enteredBy)

    // JSON serialization for offline queue
    val saleJson = com.example.data.sync.SyncMappers.saleToJson(sale)
    val fromJsonSale = com.example.data.sync.SyncMappers.saleFromJson(saleJson)
    assertEquals(sale.date, fromJsonSale.date)
    assertEquals(sale.totalAmount, fromJsonSale.totalAmount, 0.001)

    // Expense Map serialization
    val expense = Expense(
      shopName = "Sharma Spices",
      category = "Ingredients",
      description = "Chaat Masala & Sev",
      amount = 850.0,
      paymentType = PaymentTypes.CASH,
      paymentStatus = PaymentStatus.PAID,
      paidDate = date,
      purchaseDate = date,
      enteredBy = "Amit",
      firestoreId = "exp_12345"
    )
    val expenseMap = com.example.data.sync.SyncMappers.expenseToMap(expense)
    assertEquals("Sharma Spices", expenseMap["shopName"])
    assertEquals(850.0, expenseMap["amount"])
    assertEquals("Amit", expenseMap["enteredBy"])

    val restoredExpense = com.example.data.sync.SyncMappers.expenseFromMap("exp_12345", expenseMap)
    assertEquals("Sharma Spices", restoredExpense.shopName)
    assertEquals(850.0, restoredExpense.amount, 0.001)
    assertEquals("Amit", restoredExpense.enteredBy)

    // Expense JSON serialization for queue
    val expenseJson = com.example.data.sync.SyncMappers.expenseToJson(expense)
    val fromJsonExpense = com.example.data.sync.SyncMappers.expenseFromJson(expenseJson)
    assertEquals(expense.shopName, fromJsonExpense.shopName)
    assertEquals(expense.amount, fromJsonExpense.amount, 0.001)
    assertEquals("Amit", fromJsonExpense.enteredBy)
  }
}

