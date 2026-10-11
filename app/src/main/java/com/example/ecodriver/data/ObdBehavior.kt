package com.example.ecodriver.data

import com.github.eltonvs.obd.command.AdaptiveTimingMode
import com.github.eltonvs.obd.command.ObdProtocols
import com.github.eltonvs.obd.command.Switcher
import com.github.eltonvs.obd.command.at.ResetAdapterCommand
import com.github.eltonvs.obd.command.at.SelectProtocolCommand
import com.github.eltonvs.obd.command.at.SetAdaptiveTimingCommand
import com.github.eltonvs.obd.command.at.SetEchoCommand
import com.github.eltonvs.obd.command.at.SetHeadersCommand
import com.github.eltonvs.obd.command.at.SetLineFeedCommand
import com.github.eltonvs.obd.command.at.SetSpacesCommand
import com.github.eltonvs.obd.command.control.TroubleCodesCommand
import com.github.eltonvs.obd.command.control.VINCommand
import com.github.eltonvs.obd.command.engine.RPMCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import java.io.InputStream
import java.io.OutputStream

/* Not yet implemented */
suspend fun readObd(inputStream: InputStream, outputStream: OutputStream) {
    val obdConnection = ObdDeviceConnection(inputStream, outputStream)

    // Recommended low-noise ELM327 setup
    obdConnection.run(ResetAdapterCommand())
    obdConnection.run(SetEchoCommand(Switcher.OFF))
    obdConnection.run(SetLineFeedCommand(Switcher.OFF))
    obdConnection.run(SetSpacesCommand(Switcher.OFF))
    obdConnection.run(SetHeadersCommand(Switcher.OFF))
    obdConnection.run(SetAdaptiveTimingCommand(AdaptiveTimingMode.AUTO_1))
    obdConnection.run(SelectProtocolCommand(ObdProtocols.AUTO))

    val rpm = obdConnection.run(RPMCommand())
    val vin = obdConnection.run(VINCommand(), useCache = true)
    val troubleCodes = obdConnection.run(TroubleCodesCommand())

    println("RPM: ${rpm.value} ${rpm.unit}")
    println("VIN: ${vin.value}")
    println("DTC: ${troubleCodes.value.ifBlank { "none" }}")
}