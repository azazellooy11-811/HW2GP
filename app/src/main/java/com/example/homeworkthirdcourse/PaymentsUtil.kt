package com.example.homeworkthirdcourse

import android.app.Activity
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode


object PaymentsUtil {
        const val PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST
        val SUPPORTED_NETWORKS = listOf(
            "AMEX",
            "DISCOVER",
            "JCB",
            "MASTERCARD",
            "VISA")
        val SUPPORTED_METHODS = listOf(
            "PAN_ONLY",
            "CRYPTOGRAM_3DS")
        const val COUNTRY_CODE = "US"
        const val CURRENCY_CODE = "USD"
        val SHIPPING_SUPPORTED_COUNTRIES = listOf("RU", "UK", "US")

        const val PAYMENT_GATEWAY_TOKENIZATION_NAME = "sberbank"
        const val PAYMENT_GATEWAY_MERCHANT_ID = "Homework App"
        const val PAYMENT_TOTAL_PRICE= "0.1"
        const val PAYMENT_CURRENCY_CODE = "USD"
        val PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS = mapOf(
            "gateway" to PAYMENT_GATEWAY_TOKENIZATION_NAME,
            "gatewayMerchantId" to PAYMENT_GATEWAY_MERCHANT_ID
        )
        val PAYMENT_TRANSACTION_PARAMETERS = mapOf(
            "totalPrice" to PAYMENT_TOTAL_PRICE,
            "currencyCode" to PAYMENT_CURRENCY_CODE
        )
        const val DIRECT_TOKENIZATION_PUBLIC_KEY = "REPLACE_ME"
        val DIRECT_TOKENIZATION_PARAMETERS = mapOf(
            "protocolVersion" to "ECv1",
            "publicKey" to DIRECT_TOKENIZATION_PUBLIC_KEY
        )
        val MICROS = BigDecimal(1000000.0)

    private val baseRequest = JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
    }

    private fun gatewayTokenizationSpecification(): JSONObject {
        if (PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS.isEmpty()) {
            throw RuntimeException(
                "Please edit the Constants.java file to add gateway name and other " +
                        "parameters your processor requires")
        }

        return JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put("parameters", JSONObject(PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS))
        }
    }

    private val allowedCardNetworks = JSONArray(SUPPORTED_NETWORKS)

    private val allowedCardAuthMethods = JSONArray(SUPPORTED_METHODS)

    private fun baseCardPaymentMethod(): JSONObject {
        return JSONObject().apply {

            val parameters = JSONObject().apply {
                put("allowedAuthMethods", allowedCardAuthMethods)
                put("allowedCardNetworks", allowedCardNetworks)
                put("billingAddressRequired", true)
                put("billingAddressParameters", JSONObject().apply {
                    put("format", "FULL")
                })
            }

            put("type", "CARD")
            put("parameters", parameters)
        }
    }

    private fun cardPaymentMethod(): JSONObject {
        val cardPaymentMethod = baseCardPaymentMethod()
        cardPaymentMethod.put("tokenizationSpecification", gatewayTokenizationSpecification())

        return cardPaymentMethod
    }

    fun isReadyToPayRequest(): JSONObject? {
        return try {
            val isReadyToPayRequest = JSONObject(baseRequest.toString())
            isReadyToPayRequest.put(
                "allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))

            isReadyToPayRequest

        } catch (e: JSONException) {
            null
        }
    }

    private val merchantInfo: JSONObject
        @Throws(JSONException::class)
        get() = JSONObject().put("merchantName", "OAO Android Homework")

    fun createPaymentsClient(activity: Activity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(PAYMENTS_ENVIRONMENT)
            .build()

        return Wallet.getPaymentsClient(activity, walletOptions)
    }

    @Throws(JSONException::class)
    private fun getTransactionInfo(price: String): JSONObject {
        return JSONObject().apply {
            put("totalPrice", price)
            put("totalPriceStatus", "FINAL")
            put("countryCode", COUNTRY_CODE)
            put("currencyCode", CURRENCY_CODE)
        }
    }

    fun getPaymentDataRequest(price: String): JSONObject? {
        try {
            return JSONObject(baseRequest.toString()).apply {
                put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod()))
                put("transactionInfo", getTransactionInfo(price))
                put("merchantInfo", merchantInfo)

                val shippingAddressParameters = JSONObject().apply {
                    put("phoneNumberRequired", false)
                    put("allowedCountryCodes", JSONArray(SHIPPING_SUPPORTED_COUNTRIES))
                }
                put("shippingAddressRequired", true)
                put("shippingAddressParameters", shippingAddressParameters)
            }
        } catch (e: JSONException) {
            return null
        }

    }
}

fun Long.microsToString() = BigDecimal(this)
    .divide(PaymentsUtil.MICROS)
    .setScale(2, RoundingMode.HALF_EVEN)
    .toString()