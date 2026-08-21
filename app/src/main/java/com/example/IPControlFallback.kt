package com.example

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class IPControlFallback(private val tvIpAddress: String, private val preSharedKey: String) {

    // Network Fallback: Sony IRCC-IP Details
    suspend fun sendIrccCommand(commandBase64: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$tvIpAddress/sony/ircc")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8")
            connection.setRequestProperty("SOAPAction", "\"urn:schemas-sony-com:service:IRCC:1#X_SendIRCC\"")
            connection.setRequestProperty("X-Auth-PSK", preSharedKey)
            connection.doOutput = true

            val soapBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                    <s:Body>
                        <u:X_SendIRCC xmlns:u="urn:schemas-sony-com:service:IRCC:1">
                            <IRCCCode>$commandBase64</IRCCCode>
                        </u:X_SendIRCC>
                    </s:Body>
                </s:Envelope>
            """.trimIndent()

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(soapBody)
            writer.flush()
            writer.close()

            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
    
    // IRCC Codes for Sony TVs
    object Commands {
        const val HOME = "AAAAAQAAAAEAAABgAw=="
        const val ENTER = "AAAAAQAAAAEAAAB1Aw=="
        const val UP = "AAAAAQAAAAEAAAB0Aw=="
        const val DOWN = "AAAAAQAAAAEAAAB1Aw==" // Verify code
        const val LEFT = "AAAAAQAAAAEAAAA0Aw=="
        const val RIGHT = "AAAAAQAAAAEAAAAzAw=="
    }
}
