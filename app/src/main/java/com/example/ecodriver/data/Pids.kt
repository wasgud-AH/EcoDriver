package com.example.ecodriver.data

import com.github.eltonvs.obd.command.control.DTCNumberCommand
import com.github.eltonvs.obd.command.control.DistanceSinceCodesClearedCommand
import com.github.eltonvs.obd.command.control.MILOnCommand
import com.github.eltonvs.obd.command.control.ModuleVoltageCommand
import com.github.eltonvs.obd.command.control.PendingTroubleCodesCommand
import com.github.eltonvs.obd.command.control.PermanentTroubleCodesCommand
import com.github.eltonvs.obd.command.control.TimeSinceCodesClearedCommand
import com.github.eltonvs.obd.command.control.TroubleCodesCommand
import com.github.eltonvs.obd.command.control.VINCommand
import com.github.eltonvs.obd.command.engine.LoadCommand
import com.github.eltonvs.obd.command.engine.MassAirFlowCommand
import com.github.eltonvs.obd.command.engine.RPMCommand
import com.github.eltonvs.obd.command.engine.SpeedCommand
import com.github.eltonvs.obd.command.engine.ThrottlePositionCommand
import com.github.eltonvs.obd.command.fuel.FuelConsumptionRateCommand
import com.github.eltonvs.obd.command.fuel.FuelTrimCommand
import com.github.eltonvs.obd.command.fuel.FuelTrimCommand.FuelTrimBank
import com.github.eltonvs.obd.command.pressure.IntakeManifoldPressureCommand
import com.github.eltonvs.obd.command.temperature.EngineCoolantTemperatureCommand

// obdValues / obdNumbers keys. Values are in the library's units (km/h, °C, kPa, km).
object Pid {
    val SPEED = SpeedCommand().name
    val RPM = RPMCommand().name
    val THROTTLE = ThrottlePositionCommand().name
    val MAF = MassAirFlowCommand().name
    val FUEL_RATE = FuelConsumptionRateCommand().name
    val LOAD = LoadCommand().name
    val COOLANT = EngineCoolantTemperatureCommand().name
    val MAP = IntakeManifoldPressureCommand().name
    val STFT_B1 = FuelTrimCommand(FuelTrimBank.SHORT_TERM_BANK_1).name
    val LTFT_B1 = FuelTrimCommand(FuelTrimBank.LONG_TERM_BANK_1).name
    val VOLTAGE = ModuleVoltageCommand().name
    val VIN = VINCommand().name
    val MIL = MILOnCommand().name
    val DTC_COUNT = DTCNumberCommand().name
    val DTC_STORED = TroubleCodesCommand().name
    val DTC_PENDING = PendingTroubleCodesCommand().name
    val DTC_PERMANENT = PermanentTroubleCodesCommand().name
    val KM_SINCE_CLEAR = DistanceSinceCodesClearedCommand().name
    val MIN_SINCE_CLEAR = TimeSinceCodesClearedCommand().name
}
