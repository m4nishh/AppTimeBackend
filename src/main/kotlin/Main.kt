import com.apptime.code.module
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.util.*

fun main() {
    // Set JVM timezone to IST (Asia/Kolkata)
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
    System.setProperty("user.timezone", "Asia/Kolkata")
    
    println("🌏 Server timezone set to: ${TimeZone.getDefault().id} (${TimeZone.getDefault().displayName})")
    
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}