package com.example.data.api

import com.example.BuildConfig
import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import com.example.domain.FinancialReport
import com.example.domain.ShopProfile
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private val moshi = try {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    } catch (_: Throwable) {
        Moshi.Builder().build()
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: GeminiApi = retrofit.create(GeminiApi::class.java)

    suspend fun consultAdvisor(
        userPrompt: String,
        report: FinancialReport,
        products: List<ProductEntity>,
        customersWithDue: List<CustomerEntity>,
        shop: ShopProfile = ShopProfile(),
        preferHindi: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                !apiKey.contains("PLACEHOLDER")

        // First prepare the exact financial ground-truth data from live DB
        val totalDue = customersWithDue.sumOf { it.outstandingBalance }
        val lowStockItems = products.filter { it.stockQuantity <= it.lowStockThreshold }

        if (hasValidKey) {
            try {
                val systemContext = """
                    You are ASLAM AI, the personal shop business advisor, accountant and smart manager for ${shop.name} (${shop.tagline}).
                    
                    ROLE & CAPABILITIES:
                    - Advise the owner on business profits, optimal selling prices, margins, savings, and inventory management.
                    - Suggest best selling price: analyze buy price vs market rates to maximize profit while retaining customers.
                    - Respond in clean, professional English or warm Hinglish depending on what the user asks.
                    - Keep formatting clean with bullet points and bold headers.
                    
                    CRITICAL FORMATTING RULES:
                    - NEVER use dollar signs ($) or double dollars ($$). Always use ₹ for Indian Rupees.
                    - NEVER use LaTeX math notation or weird signs like -$$ or \frac{} or \text{}.
                    - Always write numbers cleanly, e.g., ₹250 or ₹1,500.50.
                    
                    CURRENT LIVE SHOP DATA:
                    - Total Sales Revenue: ₹${String.format(Locale.US, "%.2f", report.totalSalesRevenue)}
                    - Cash/UPI Payments Received: ₹${String.format(Locale.US, "%.2f", report.totalCashReceived)}
                    - Cost of Goods Sold (COGS): ₹${String.format(Locale.US, "%.2f", report.cogs)}
                    - Recorded Shop Expenses: ₹${String.format(Locale.US, "%.2f", report.totalExpenses)}
                    - Net Business Profit: ₹${String.format(Locale.US, "%.2f", report.netProfit)}
                    - Total Customer Udhar (Outstanding): ₹${String.format(Locale.US, "%.2f", totalDue)}
                    - Customers with pending udhar: ${customersWithDue.take(10).joinToString { "${it.name}: ₹${it.outstandingBalance}" }}
                    - Low Stock Products: ${lowStockItems.joinToString { "${it.name} (${it.stockQuantity} ${it.unit} left, Min: ${it.lowStockThreshold})" }}
                    - Total Products in Database: ${products.size}
                    - Product Catalog with margins: ${products.take(10).joinToString { "${it.name}: Buy ₹${it.purchasePrice}, Sell ₹${it.sellingPrice}, Margin ₹${it.sellingPrice - it.purchasePrice}" }}
                    
                    USER QUERY: $userPrompt
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = systemContext))
                        )
                    )
                )

                val response = api.generateContent(apiKey, request)
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!reply.isNullOrBlank()) {
                    // Thoroughly clean any stray dollar, negative or latex symbols
                    val cleaned = reply
                        .replace("$$", "₹")
                        .replace("$", "₹")
                        .replace("₹-", "- ₹")
                        .replace("-₹", "- ₹")
                        .replace("\\text{", "")
                        .replace("\\frac{", "")
                        .replace("}", "")
                        .replace("{", "")
                        .replace("\\", "")
                    return@withContext cleaned
                }
            } catch (_: Exception) {
                // Fallback smoothly to local calculation engine
            }
        }

        // Reliable local calculation analysis engine (Works 100% offline & without API key)
        return@withContext localAnalysis(userPrompt, report, products, customersWithDue, hasValidKey)
    }

    private fun localAnalysis(
        prompt: String,
        report: FinancialReport,
        products: List<ProductEntity>,
        customersWithDue: List<CustomerEntity>,
        hasValidKey: Boolean
    ): String {
        val lower = prompt.lowercase(Locale.ROOT)
        val sb = StringBuilder()

        val totalDue = customersWithDue.sumOf { it.outstandingBalance }
        val lowStock = products.filter { it.stockQuantity <= it.lowStockThreshold }

        if (lower.contains("price") || lower.contains("sell") || lower.contains("rate") || lower.contains("बिक्री भाव") || lower.contains("कितने में") || lower.contains("suggest")) {
            sb.append("💡 **Selling Price & Margin Recommendation**\n\n")
            if (products.isEmpty()) {
                sb.append("No products added yet. Please add products with Buy and Sell prices to view profit suggestions.\n")
            } else {
                sb.append("Here is the margin analysis and suggested selling prices for your inventory:\n\n")
                products.take(6).forEach { p ->
                    val margin = p.sellingPrice - p.purchasePrice
                    val marginPercent = if (p.purchasePrice > 0) (margin / p.purchasePrice) * 100 else 0.0
                    val suggestedPrice = if (p.purchasePrice > 0) p.purchasePrice * 1.25 else p.sellingPrice * 1.15
                    sb.append("• **${p.name}**\n")
                    sb.append("   - Buy Price: ₹${String.format(Locale.US, "%.2f", p.purchasePrice)}\n")
                    sb.append("   - Current Sell Price: ₹${String.format(Locale.US, "%.2f", p.sellingPrice)}\n")
                    sb.append("   - Your Profit/Savings: ₹${String.format(Locale.US, "%.2f", margin)} (${String.format(Locale.US, "%.1f", marginPercent)}%)\n")
                    sb.append("   - 🎯 Recommended Selling Price: ₹${String.format(Locale.US, "%.2f", suggestedPrice)} (gives 25% healthy margin)\n\n")
                }
                sb.append("💡 **Pro Tip:** Keep fast-moving items at 15-20% margin for quick turnover, and specialty tobacco/premium items at 25-30% margin.")
            }
        } else if (lower.contains("profit") || lower.contains("saving") || lower.contains("बचत") || lower.contains("मुनाफा") || lower.contains("फायदा")) {
            sb.append("💰 **Profit & Savings Summary (${report.periodLabel})**\n\n")
            sb.append("1. Total Sales Revenue: ₹${String.format(Locale.US, "%.2f", report.totalSalesRevenue)}\n")
            sb.append("2. Cost of Goods Sold (Purchase Cost): ₹${String.format(Locale.US, "%.2f", report.cogs)}\n")
            sb.append("3. Gross Profit (Revenue - Cost): ₹${String.format(Locale.US, "%.2f", report.grossProfit)}\n")
            sb.append("4. Shop Expenses: ₹${String.format(Locale.US, "%.2f", report.totalExpenses)}\n")
            sb.append("───────────────────────\n")
            val status = if (report.netProfit >= 0) "✅ Net Business Profit" else "⚠️ Net Loss"
            sb.append("$status: ₹${String.format(Locale.US, "%.2f", report.netProfit)}\n\n")
            if (report.totalSalesRevenue > 0) {
                val netMargin = (report.netProfit / report.totalSalesRevenue) * 100
                sb.append("📈 Net Profit Margin: ${String.format(Locale.US, "%.1f", netMargin)}%\n")
            }
            sb.append("💡 Calculated accurately based on your actual sale and purchase records.")
        } else if (lower.contains("sale") || lower.contains("revenue") || lower.contains("बिक्री") || lower.contains("कमाई")) {
            sb.append("📊 **Sales Report (${report.periodLabel})**\n\n")
            sb.append("• Total Sales: ₹${String.format(Locale.US, "%.2f", report.totalSalesRevenue)}\n")
            sb.append("• Cash/UPI Received: ₹${String.format(Locale.US, "%.2f", report.totalCashReceived)}\n")
            sb.append("• Total Invoices: ${report.salesCount}\n\n")
            if (report.totalSalesRevenue > report.totalCashReceived) {
                val pending = report.totalSalesRevenue - report.totalCashReceived
                sb.append("⚠️ Pending Credit / Udhar: ₹${String.format(Locale.US, "%.2f", pending)}")
            } else {
                sb.append("✅ All recorded sales are fully paid!")
            }
        } else if (lower.contains("udhar") || lower.contains("due") || lower.contains("credit") || lower.contains("बाकी") || lower.contains("बकाया")) {
            sb.append("👥 **Customer Outstanding Khata Ledger**\n\n")
            sb.append("• Total Outstanding Due: ₹${String.format(Locale.US, "%.2f", totalDue)}\n")
            sb.append("• Customers with Due: ${customersWithDue.size}\n\n")
            if (customersWithDue.isEmpty()) {
                sb.append("🎉 Great! No customers currently have outstanding balances.")
            } else {
                sb.append("Top Customers with Pending Balances:\n")
                customersWithDue.take(6).forEachIndexed { i, c ->
                    sb.append("${i + 1}. ${c.name} (${c.phone}): ₹${String.format(Locale.US, "%.2f", c.outstandingBalance)}\n")
                }
                sb.append("\n💡 **Action:** Tap the WhatsApp icon on the Customers screen to send an instant payment reminder.")
            }
        } else if (lower.contains("stock") || lower.contains("item") || lower.contains("inventory") || lower.contains("सामान")) {
            sb.append("📦 **Stock & Inventory Status**\n\n")
            sb.append("• Total Products: ${products.size}\n")
            sb.append("• Stock Value at Cost: ₹${String.format(Locale.US, "%.2f", report.inventoryCostValue)}\n")
            sb.append("• Estimated Retail Value: ₹${String.format(Locale.US, "%.2f", report.inventoryRetailValue)}\n\n")
            if (lowStock.isEmpty()) {
                sb.append("✅ All products have sufficient stock!")
            } else {
                sb.append("⚠️ **Low Stock Warning (${lowStock.size} items):**\n")
                lowStock.forEach { p ->
                    sb.append("• ${p.name}: only ${p.stockQuantity} ${p.unit} remaining (Reorder threshold: ${p.lowStockThreshold})\n")
                }
                sb.append("\n💡 **Recommendation:** Place an order with your supplier to avoid stockouts.")
            }
        } else {
            sb.append("Hello Aslam Bhai! I am ASLAM AI, your digital shop accountant.\n\n")
            sb.append("Current Shop Health:\n")
            sb.append("• Today's Sales: ₹${String.format(Locale.US, "%.2f", report.totalSalesRevenue)}\n")
            sb.append("• Cash Received: ₹${String.format(Locale.US, "%.2f", report.totalCashReceived)}\n")
            sb.append("• Total Udhar Due: ₹${String.format(Locale.US, "%.2f", totalDue)}\n")
            sb.append("• Net Business Profit: ₹${String.format(Locale.US, "%.2f", report.netProfit)}\n")
            sb.append("• Low Stock Items: ${lowStock.size}\n\n")
            sb.append("Quick Suggestions:\n")
            sb.append("• 'What selling price do you recommend for higher profit?'\n")
            sb.append("• 'Show today's profit and savings breakdown'\n")
            sb.append("• 'Who owes us udhar money?'\n")
            sb.append("• 'Which items need restocking?'")
        }

        if (!hasValidKey) {
            sb.append("\n\n───────────────────────\n")
            sb.append("ℹ️ *To enable real-time Gemini reasoning, add your GEMINI_API_KEY in the AI Studio Secrets panel.*")
        }

        return sb.toString()
    }
}
