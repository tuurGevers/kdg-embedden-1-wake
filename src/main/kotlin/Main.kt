import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.*
import java.io.PrintWriter
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@OptIn(DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
    val portName = "COM3" // Replace with the appropriate port name for your system
    val port = SerialPort.getCommPort(portName)
    port.openPort()

    val buffer = ByteArray(1024)
    var currentRate = 0.0
    var combinedValue = ""

    while (true) {
        val bytesRead = port.readBytes(buffer, buffer.size.toLong())
        if (bytesRead > 0) {
            val receivedData = buffer.copyOf(bytesRead.toInt())
            val decodedData = String(receivedData, StandardCharsets.UTF_8)
            if(decodedData ==":"){
                sendAwake();
            }else{
                for (char in decodedData) {
                    if (char != '.') {
                        combinedValue += char
                    } else {
                        if (combinedValue.isNotEmpty()) {
                            currentRate = combinedValue.toDouble()
                            combinedValue = ""
                            println(currentRate)

                            // Send data asynchronously
                            GlobalScope.launch(Dispatchers.IO) {
                                sendData(currentRate)
                                val time =getTime()
                                val writer = PrintWriter(port.outputStream)
                                writer.print(time)
                                writer.close()
                            }
                        }
                    }
                }
            }

        }
    }
}

fun sendAwake() {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/home/wake"))
        .POST(HttpRequest.BodyPublishers.ofString("awake"))
        .build()

    val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())}

fun sendData(rate: Double) {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/home/"))
        .POST(HttpRequest.BodyPublishers.ofString(rate.toString()))
        .build()

    val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    response.join()
    println(response.get().body())
}

fun getTime():Int {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/home/time"))
        .build()

    val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    response.join()
    val responseBody = response.get().body()
    return responseBody.toInt()
}

fun numberToByteArray(data: Number, size: Int = 4): ByteArray =
    ByteArray(size) { i -> (data.toLong() shr (i * 8)).toByte() }
